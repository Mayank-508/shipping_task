-- Customers
INSERT INTO customer (id, name, phone, latitude, longitude) VALUES (1, 'Ramesh Kirana Store (Bangalore)', '9876543210', 12.9716, 77.5946);
INSERT INTO customer (id, name, phone, latitude, longitude) VALUES (2, 'Suresh Traders (Mysore)', '8765432109', 12.2958, 76.6394);

-- Sellers
INSERT INTO seller (id, name, latitude, longitude) VALUES (1, 'MegaMart Wholesale (Tumkur)', 13.3391, 77.1010);
INSERT INTO seller (id, name, latitude, longitude) VALUES (2, 'North India Distributors (Delhi)', 28.7041, 77.1025);

-- Warehouses
INSERT INTO warehouse (id, latitude, longitude) VALUES (1, 13.0000, 77.5000); -- Near Bangalore
INSERT INTO warehouse (id, latitude, longitude) VALUES (2, 28.6000, 77.2000); -- Near Delhi
INSERT INTO warehouse (id, latitude, longitude) VALUES (3, 19.0760, 72.8777); -- Near Mumbai

-- Products for MegaMart Wholesale (Tumkur) [Seller 1]
INSERT INTO product (id, seller_id, weight, dimensions) VALUES (1, 1, 0.5, '10x10x5');   -- Light packet
INSERT INTO product (id, seller_id, weight, dimensions) VALUES (2, 1, 10.0, '50x50x30');  -- Medium box

-- Products for North India Distributors (Delhi) [Seller 2]
INSERT INTO product (id, seller_id, weight, dimensions) VALUES (3, 2, 25.0, '100x80x50'); -- Heavy carton
INSERT INTO product (id, seller_id, weight, dimensions) VALUES (4, 2, 2.0, '20x20x20');   -- Small box
