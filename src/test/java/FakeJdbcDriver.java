import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

final class FakeJdbcDriver implements Driver {
    private static final String JDBC_PREFIX = "jdbc:mariadb://";
    private static final FakeJdbcDriver INSTANCE = new FakeJdbcDriver();

    private static ConnectionFactory connectionFactory;

    private FakeJdbcDriver() {
    }

    static void install(ConnectionFactory factory) throws SQLException {
        connectionFactory = factory;
        deregisterMariaDbDrivers();
        if (!isRegistered()) {
            DriverManager.registerDriver(INSTANCE);
        }
    }

    static void uninstall() throws SQLException {
        if (isRegistered()) {
            DriverManager.deregisterDriver(INSTANCE);
        }
        connectionFactory = null;
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url) || connectionFactory == null) {
            return null;
        }
        return connectionFactory.create(url, info);
    }

    @Override
    public boolean acceptsURL(String url) {
        return url != null && url.startsWith(JDBC_PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("Logging is not supported.");
    }

    private static boolean isRegistered() throws SQLException {
        var drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            if (drivers.nextElement() == INSTANCE) {
                return true;
            }
        }
        return false;
    }

    private static void deregisterMariaDbDrivers() throws SQLException {
        var drivers = DriverManager.getDrivers();
        List<Driver> toRemove = new ArrayList<>();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver != INSTANCE && driver.getClass().getName().startsWith("org.mariadb")) {
                toRemove.add(driver);
            }
        }

        for (Driver driver : toRemove) {
            DriverManager.deregisterDriver(driver);
        }
    }

    interface ConnectionFactory {
        Connection create(String url, Properties info) throws SQLException;
    }

    static final class ConnectionState {
        boolean autoCommit = true;
        boolean closed;
        int commitCount;
        int rollbackCount;
        boolean rollbackFails;
        String capturedUrl;
        String capturedUser;
        String capturedPassword;
        final Map<String, PreparedStatementState> statements = new HashMap<>();

        Connection toConnection() {
            InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
                case "getAutoCommit" -> autoCommit;
                case "setAutoCommit" -> {
                    autoCommit = (boolean) args[0];
                    yield null;
                }
                case "prepareStatement" -> prepareStatement((String) args[0], args.length > 1);
                case "commit" -> {
                    commitCount++;
                    yield null;
                }
                case "rollback" -> {
                    rollbackCount++;
                    if (rollbackFails) {
                        throw new SQLException("Rollback failed");
                    }
                    yield null;
                }
                case "close" -> {
                    closed = true;
                    yield null;
                }
                case "isClosed" -> closed;
                case "unwrap" -> proxy;
                case "isWrapperFor" -> false;
                default -> defaultValue(method.getReturnType());
            };

            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class[]{Connection.class},
                    handler
            );
        }

        private PreparedStatement prepareStatement(String sql, boolean withGeneratedKeys) throws SQLException {
            PreparedStatementState state = statements.get(sql);
            if (state == null) {
                throw new SQLException("Unexpected SQL: " + sql);
            }
            state.withGeneratedKeys = withGeneratedKeys;
            return state.toPreparedStatement();
        }
    }

    static final class PreparedStatementState {
        final Map<Integer, Object> currentParameters = new HashMap<>();
        final List<Map<Integer, Object>> batches = new ArrayList<>();
        List<Map<?, ?>> queryRows = List.of();
        List<Map<?, ?>> generatedKeyRows = List.of();
        boolean withGeneratedKeys;
        boolean executeUpdateCalled;
        boolean executeBatchCalled;
        SQLException executeUpdateException;
        SQLException executeBatchException;
        QueryRowsProvider queryRowsProvider;

        PreparedStatement toPreparedStatement() {
            InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
                case "setInt", "setDouble", "setString" -> {
                    currentParameters.put((Integer) args[0], args[1]);
                    yield null;
                }
                case "addBatch" -> {
                    batches.add(new HashMap<>(currentParameters));
                    yield null;
                }
                case "executeBatch" -> executeBatch();
                case "executeUpdate" -> executeUpdate();
                case "executeQuery" -> createQueryResultSet();
                case "getGeneratedKeys" -> createGeneratedKeysResultSet();
                case "close" -> null;
                case "unwrap" -> proxy;
                case "isWrapperFor" -> false;
                default -> defaultValue(method.getReturnType());
            };

            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class[]{PreparedStatement.class},
                    handler
            );
        }

        private int[] executeBatch() throws SQLException {
            executeBatchCalled = true;
            if (executeBatchException != null) {
                throw executeBatchException;
            }
            return batches.stream().mapToInt(ignored -> Statement.SUCCESS_NO_INFO).toArray();
        }

        private int executeUpdate() throws SQLException {
            executeUpdateCalled = true;
            if (executeUpdateException != null) {
                throw executeUpdateException;
            }
            return 1;
        }

        private ResultSet createQueryResultSet() {
            List<Map<?, ?>> rows = queryRowsProvider == null
                    ? queryRows
                    : queryRowsProvider.getRows(Map.copyOf(currentParameters));
            return resultSet(rows);
        }

        private ResultSet createGeneratedKeysResultSet() {
            return resultSet(generatedKeyRows);
        }
    }

    interface QueryRowsProvider {
        List<Map<?, ?>> getRows(Map<Integer, Object> parameters);
    }

    private static ResultSet resultSet(List<Map<?, ?>> rows) {
        InvocationHandler handler = new InvocationHandler() {
            int index = -1;

            @Override
            public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                return switch (method.getName()) {
                    case "next" -> ++index < rows.size();
                    case "getString" -> Objects.toString(rows.get(index).get(args[0]), null);
                    case "getInt" -> ((Number) rows.get(index).get(args[0])).intValue();
                    case "close" -> null;
                    case "unwrap" -> proxy;
                    case "isWrapperFor" -> false;
                    default -> defaultValue(method.getReturnType());
                };
            }
        };

        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class[]{ResultSet.class},
                handler
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == double.class) {
            return 0.0d;
        }
        if (returnType == float.class) {
            return 0.0f;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }
}
