INSERT INTO categories (name, status) VALUES
                                          ('General', 'ACTIVE'),
                                          ('Food',    'ACTIVE'),
                                          ('Drinks',  'ACTIVE');
INSERT INTO products (sku, name, category_id, price, stock, status)
VALUES
    ('GEN-0001', 'General Item 1',
     (SELECT id FROM categories WHERE name='General'),  99000,  10, 'ACTIVE'),
    ('GEN-0002', 'General Item 2',
     (SELECT id FROM categories WHERE name='General'), 149000,  5,  'ACTIVE'),
    ('GEN-0003', 'General Item 3',
     (SELECT id FROM categories WHERE name='General'), 199000,  7,  'ACTIVE'),
    ('GEN-0004', 'General Item 4',
     (SELECT id FROM categories WHERE name='General'), 249000, 12,  'ACTIVE'),
    ('GEN-0005', 'General Item 5',
     (SELECT id FROM categories WHERE name='General'), 299000,  3,  'ACTIVE');

INSERT INTO products (sku, name, category_id, price, stock, status)
VALUES
    ('FOOD-0001', 'Noodles Pack',
     (SELECT id FROM categories WHERE name='Food'),     12000,  50, 'ACTIVE'),
    ('FOOD-0002', 'Canned Tuna',
     (SELECT id FROM categories WHERE name='Food'),     35000,  30, 'ACTIVE'),
    ('FOOD-0003', 'Chocolate Bar',
     (SELECT id FROM categories WHERE name='Food'),     18000,  80, 'ACTIVE'),
    ('FOOD-0004', 'Rice 5kg',
     (SELECT id FROM categories WHERE name='Food'),    120000,  20, 'ACTIVE'),
    ('FOOD-0005', 'Instant Soup',
     (SELECT id FROM categories WHERE name='Food'),     15000,  60, 'ACTIVE');

INSERT INTO products (sku, name, category_id, price, stock, status)
VALUES
    ('DRK-0001', 'Mineral Water 500ml',
     (SELECT id FROM categories WHERE name='Drinks'),   8000,   100, 'ACTIVE'),
    ('DRK-0002', 'Orange Juice 1L',
     (SELECT id FROM categories WHERE name='Drinks'),   45000,  40,  'ACTIVE'),
    ('DRK-0003', 'Cola 330ml',
     (SELECT id FROM categories WHERE name='Drinks'),   12000,  120, 'ACTIVE'),
    ('DRK-0004', 'Green Tea 500ml',
     (SELECT id FROM categories WHERE name='Drinks'),   15000,  70,  'ACTIVE'),
    ('DRK-0005', 'Coffee Can 240ml',
     (SELECT id FROM categories WHERE name='Drinks'),   18000,  90,  'ACTIVE');
