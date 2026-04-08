CREATE DATABASE IF NOT EXISTS shopping_cart_localization
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE shopping_cart_localization;

CREATE TABLE IF NOT EXISTS cart_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    total_items INT NOT NULL,
    total_cost DOUBLE NOT NULL,
    language VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cart_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cart_record_id INT,
    item_number INT NOT NULL,
    price DOUBLE NOT NULL,
    quantity INT NOT NULL,
    subtotal DOUBLE NOT NULL,
    FOREIGN KEY (cart_record_id) REFERENCES cart_records(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS localization_strings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    `key` VARCHAR(100) NOT NULL,
    value VARCHAR(255) NOT NULL,
    language VARCHAR(10) NOT NULL
);

INSERT INTO localization_strings (`key`, value, language) VALUES
('label.language', 'Select the language:', 'en_US'),
('prompt.items', 'Enter number of items:', 'en_US'),
('prompt.item_price', 'Enter price for item', 'en_US'),
('prompt.item_quantity', 'Enter quantity for item', 'en_US'),
('result', 'Total:', 'en_US'),
('button.generate', 'Enter items', 'en_US'),
('button.calculate', 'Calculate Total', 'en_US'),
('error.title', 'Error', 'en_US'),
('error.invalid_number', 'Please enter a valid number of items.', 'en_US'),
('error.invalid_input', 'Please enter valid price and quantity values.', 'en_US'),
('error.database', 'Database operation failed. Please check your database connection.', 'en_US'),
('label.language', 'Valitse kieli:', 'fi_FI'),
('prompt.items', 'Syötä ostettavien tuotteiden määrä:', 'fi_FI'),
('prompt.item_price', 'Syötä tuotteen hinta', 'fi_FI'),
('prompt.item_quantity', 'Syötä tuotteen määrä', 'fi_FI'),
('result', 'Kokonaishinta:', 'fi_FI'),
('button.generate', 'Syötä tuotteet', 'fi_FI'),
('button.calculate', 'Laske loppusumma', 'fi_FI'),
('error.title', 'Virhe', 'fi_FI'),
('error.invalid_number', 'Anna kelvollinen määrä tuotteita.', 'fi_FI'),
('error.invalid_input', 'Anna kelvolliset hinta- ja määräarvot.', 'fi_FI'),
('error.database', 'Tietokantatoiminto epäonnistui. Tarkista tietokantayhteys.', 'fi_FI'),
('label.language', 'Välj språk:', 'sv_SE'),
('prompt.items', 'Ange antalet varor att köpa:', 'sv_SE'),
('prompt.item_price', 'Ange priset för varan', 'sv_SE'),
('prompt.item_quantity', 'Ange mängden varor', 'sv_SE'),
('result', 'Total kostnad:', 'sv_SE'),
('button.generate', 'Ange artiklar', 'sv_SE'),
('button.calculate', 'Beräkna totalsumma', 'sv_SE'),
('error.title', 'Fel', 'sv_SE'),
('error.invalid_number', 'Ange ett giltigt antal artiklar.', 'sv_SE'),
('error.invalid_input', 'Ange giltiga pris- och kvantitetsvärden.', 'sv_SE'),
('error.database', 'Databasåtgärden misslyckades. Kontrollera databasanslutningen.', 'sv_SE'),
('label.language', '言語を選択してください:', 'ja_JP'),
('prompt.items', '購入する商品の数を入力してください:', 'ja_JP'),
('prompt.item_price', '商品の価格を入力してください', 'ja_JP'),
('prompt.item_quantity', '商品の数量を入力してください', 'ja_JP'),
('result', '合計金額:', 'ja_JP'),
('button.generate', '商品を入力してください', 'ja_JP'),
('button.calculate', '合計を計算します', 'ja_JP'),
('error.title', 'エラー', 'ja_JP'),
('error.invalid_number', '有効な数量を入力してください。', 'ja_JP'),
('error.invalid_input', '有効な価格と数量を入力してください。', 'ja_JP'),
('error.database', 'データベース処理に失敗しました。接続設定を確認してください。', 'ja_JP'),
('label.language', 'اختر اللغة:', 'ar_AR'),
('prompt.items', 'أدخل عدد العناصر:', 'ar_AR'),
('prompt.item_price', 'أدخل سعر العنصر', 'ar_AR'),
('prompt.item_quantity', 'أدخل كمية العنصر', 'ar_AR'),
('result', 'الإجمالي:', 'ar_AR'),
('button.generate', 'أدخل العناصر', 'ar_AR'),
('button.calculate', 'احسب الإجمالي', 'ar_AR'),
('error.title', 'خطأ', 'ar_AR'),
('error.invalid_number', 'الرجاء إدخال عدد صحيح من العناصر.', 'ar_AR'),
('error.invalid_input', 'الرجاء إدخال سعر وكمية صحيحين.', 'ar_AR'),
('error.database', 'فشلت عملية قاعدة البيانات. يرجى التحقق من الاتصال.', 'ar_AR');
