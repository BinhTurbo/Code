-- Add 13 new categories
INSERT INTO categories (name, status) VALUES
    ('Electronics',     'ACTIVE'),
    ('Fashion',         'ACTIVE'),
    ('Books',           'ACTIVE'),
    ('Sports',          'ACTIVE'),
    ('Home & Garden',   'ACTIVE'),
    ('Toys',            'ACTIVE'),
    ('Health & Beauty', 'ACTIVE'),
    ('Automotive',      'ACTIVE'),
    ('Pet Supplies',    'ACTIVE'),
    ('Office Supplies', 'ACTIVE'),
    ('Music',           'ACTIVE'),
    ('Movies',          'ACTIVE'),
    ('Gaming',          'ACTIVE');

-- Add 30 new products across various categories
-- Electronics (5 products)
INSERT INTO products (sku, name, category_id, price, stock, status)
VALUES
    ('ELEC-0001', 'Wireless Mouse',
     (SELECT id FROM categories WHERE name='Electronics'), 45000, 50, 'ACTIVE'),
    ('ELEC-0002', 'USB-C Cable 2m',
     (SELECT id FROM categories WHERE name='Electronics'), 35000, 80, 'ACTIVE'),
    ('ELEC-0003', 'Bluetooth Headphones',
     (SELECT id FROM categories WHERE name='Electronics'), 250000, 25, 'ACTIVE'),
    ('ELEC-0004', 'Phone Charger 20W',
     (SELECT id FROM categories WHERE name='Electronics'), 150000, 40, 'ACTIVE'),
    ('ELEC-0005', 'HDMI Cable 3m',
     (SELECT id FROM categories WHERE name='Electronics'), 60000, 35, 'ACTIVE');

-- Fashion (4 products)
INSERT INTO products (sku, name, category_id, price, stock, status)
VALUES
    ('FASH-0001', 'Cotton T-Shirt',
     (SELECT id FROM categories WHERE name='Fashion'), 120000, 60, 'ACTIVE'),
    ('FASH-0002', 'Denim Jeans',
     (SELECT id FROM categories WHERE name='Fashion'), 350000, 30, 'ACTIVE'),
    ('FASH-0003', 'Sports Cap',
     (SELECT id FROM categories WHERE name='Fashion'), 80000, 45, 'ACTIVE'),
    ('FASH-0004', 'Canvas Sneakers',
     (SELECT id FROM categories WHERE name='Fashion'), 450000, 20, 'ACTIVE');

-- Books (3 products)
INSERT INTO products (sku, name, category_id, price, stock, status)
VALUES
    ('BOOK-0001', 'Programming in Java',
     (SELECT id FROM categories WHERE name='Books'), 280000, 15, 'ACTIVE'),
    ('BOOK-0002', 'The Art of War',
     (SELECT id FROM categories WHERE name='Books'), 95000, 25, 'ACTIVE'),
    ('BOOK-0003', 'Cooking Basics',
     (SELECT id FROM categories WHERE name='Books'), 150000, 18, 'ACTIVE');

-- Sports (3 products)
INSERT INTO products (sku, name, category_id, price, stock, status)
VALUES
    ('SPRT-0001', 'Yoga Mat',
     (SELECT id FROM categories WHERE name='Sports'), 180000, 35, 'ACTIVE'),
    ('SPRT-0002', 'Badminton Racket',
     (SELECT id FROM categories WHERE name='Sports'), 320000, 22, 'ACTIVE'),
    ('SPRT-0003', 'Soccer Ball',
     (SELECT id FROM categories WHERE name='Sports'), 250000, 28, 'ACTIVE');

-- Home & Garden (3 products)
INSERT INTO products (sku, name, category_id, price, stock, status)
VALUES
    ('HOME-0001', 'LED Desk Lamp',
     (SELECT id FROM categories WHERE name='Home & Garden'), 220000, 40, 'ACTIVE'),
    ('HOME-0002', 'Plant Pot Set 3pcs',
     (SELECT id FROM categories WHERE name='Home & Garden'), 95000, 50, 'ACTIVE'),
    ('HOME-0003', 'Kitchen Knife Set',
     (SELECT id FROM categories WHERE name='Home & Garden'), 380000, 18, 'ACTIVE');

-- Toys (2 products)
INSERT INTO products (sku, name, category_id, price, stock, status)
VALUES
    ('TOY-0001', 'Building Blocks 500pcs',
     (SELECT id FROM categories WHERE name='Toys'), 280000, 30, 'ACTIVE'),
    ('TOY-0002', 'Puzzle 1000pcs',
     (SELECT id FROM categories WHERE name='Toys'), 150000, 25, 'ACTIVE');

-- Health & Beauty (2 products)
INSERT INTO products (sku, name, category_id, price, stock, status)
VALUES
    ('HLTH-0001', 'Vitamin C Tablets',
     (SELECT id FROM categories WHERE name='Health & Beauty'), 120000, 100, 'ACTIVE'),
    ('HLTH-0002', 'Face Moisturizer',
     (SELECT id FROM categories WHERE name='Health & Beauty'), 180000, 45, 'ACTIVE');

-- Automotive (2 products)
INSERT INTO products (sku, name, category_id, price, stock, status)
VALUES
    ('AUTO-0001', 'Car Phone Holder',
     (SELECT id FROM categories WHERE name='Automotive'), 85000, 55, 'ACTIVE'),
    ('AUTO-0002', 'Engine Oil 4L',
     (SELECT id FROM categories WHERE name='Automotive'), 320000, 30, 'ACTIVE');

-- Pet Supplies (2 products)
INSERT INTO products (sku, name, category_id, price, stock, status)
VALUES
    ('PET-0001', 'Dog Food 5kg',
     (SELECT id FROM categories WHERE name='Pet Supplies'), 280000, 40, 'ACTIVE'),
    ('PET-0002', 'Cat Litter 10L',
     (SELECT id FROM categories WHERE name='Pet Supplies'), 120000, 50, 'ACTIVE');

-- Office Supplies (2 products)
INSERT INTO products (sku, name, category_id, price, stock, status)
VALUES
    ('OFFC-0001', 'A4 Paper 500 Sheets',
     (SELECT id FROM categories WHERE name='Office Supplies'), 65000, 100, 'ACTIVE'),
    ('OFFC-0002', 'Stapler Heavy Duty',
     (SELECT id FROM categories WHERE name='Office Supplies'), 95000, 35, 'ACTIVE');

-- Music (1 product)
INSERT INTO products (sku, name, category_id, price, stock, status)
VALUES
    ('MUS-0001', 'Guitar Strings Set',
     (SELECT id FROM categories WHERE name='Music'), 85000, 60, 'ACTIVE');

-- Movies (1 product)
INSERT INTO products (sku, name, category_id, price, stock, status)
VALUES
    ('MOV-0001', 'Classic Movies Collection DVD',
     (SELECT id FROM categories WHERE name='Movies'), 180000, 15, 'ACTIVE');

-- Gaming (1 product)
INSERT INTO products (sku, name, category_id, price, stock, status)
VALUES
    ('GAM-0001', 'Gaming Mouse Pad XL',
     (SELECT id FROM categories WHERE name='Gaming'), 120000, 45, 'ACTIVE');
