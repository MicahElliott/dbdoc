-- ACME Corp Customer Database

COMMENT ON TABLE customer IS 'The monster that holds the precious user profiles. Used by the traffic light table to track which users …';
COMMENT ON COLUMN customer.email IS 'The primary contact, taken from Salesforce’s =Project.Primary_Contact=. This is redundant with our =lead.contacs= data.';
COMMENT ON COLUMN customer.email_data_json IS '...';

COMMENT ON TABLE ops-user IS 'DEPRECATED: replaced by =agent=';

COMMENT ON TABLE order IS 'Every purchase made by =customer=s through the old portal. Note that new purchases by customer through all other systems is recorded in the =order2= table!';
COMMENT ON COLUMN order.foo IS 'arst';
COMMENT ON COLUMN order.bar IS 'qwfp';
