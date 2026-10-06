-- PostgreSQL 17 only. Run in a disposable database with psql -v ON_ERROR_STOP=1.
-- This verifies DB/SQL examples; it is not an app E2E or concurrency benchmark.
BEGIN;
CREATE FUNCTION pg_temp.assert_example(condition boolean, label text) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    IF condition IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'FAIL: %', label;
    END IF;
    RAISE NOTICE 'PASS: %', label;
END $$;

CREATE TEMP TABLE membership (
    member_id integer, team_id integer, PRIMARY KEY (member_id, team_id)
);
INSERT INTO membership VALUES (1,10), (1,20);
SELECT pg_temp.assert_example((SELECT count(*)=2 FROM membership), '01 composite PK permits two teams');
DO $$ BEGIN
    INSERT INTO membership VALUES (1,10);
    RAISE EXCEPTION 'FAIL: duplicate membership accepted';
EXCEPTION WHEN unique_violation THEN
    RAISE NOTICE 'PASS: 02 duplicate pair rejected';
END $$;

CREATE TEMP TABLE optional_values (email text UNIQUE, age integer CHECK(age>=18));
INSERT INTO optional_values VALUES (NULL,NULL), (NULL,18);
SELECT pg_temp.assert_example((SELECT count(*)=1 FROM optional_values WHERE age IS NULL), '03 CHECK accepts NULL');
SELECT pg_temp.assert_example((SELECT count(*)=2 FROM optional_values WHERE email IS NULL), '04 default UNIQUE permits multiple NULLs');
CREATE TEMP TABLE mandatory_age (age integer NOT NULL CHECK(age>=18));
DO $$ BEGIN
    INSERT INTO mandatory_age VALUES (NULL);
    RAISE EXCEPTION 'FAIL: required age NULL accepted';
EXCEPTION WHEN not_null_violation THEN
    RAISE NOTICE 'PASS: 05 NOT NULL rejects missing age';
END $$;
DO $$ BEGIN
    INSERT INTO mandatory_age VALUES (17);
    RAISE EXCEPTION 'FAIL: age17 accepted';
EXCEPTION WHEN check_violation THEN
    RAISE NOTICE 'PASS: 06 CHECK rejects age17';
END $$;

CREATE TEMP TABLE customers (id integer PRIMARY KEY);
CREATE TEMP TABLE orders (
    id integer PRIMARY KEY, customer_id integer REFERENCES customers(id),
    status text, amount integer, created_at timestamp
);
INSERT INTO customers VALUES (1),(2);
INSERT INTO orders VALUES
    (1,1,'PAID',20,'2026-01-01'),(2,1,'PAID',20,'2026-01-01'),
    (3,NULL,'PENDING',0,'2025-12-31');
SELECT pg_temp.assert_example((SELECT count(*)=1 FROM orders WHERE customer_id IS NULL), '07 nullable FK accepts NULL');
DO $$ BEGIN
    INSERT INTO orders(id,customer_id) VALUES (4,999);
    RAISE EXCEPTION 'FAIL: nonexistent parent accepted';
EXCEPTION WHEN foreign_key_violation THEN
    RAISE NOTICE 'PASS: 08 FK rejects nonexistent parent';
END $$;
DO $$ BEGIN
    DELETE FROM customers WHERE id=1;
    RAISE EXCEPTION 'FAIL: referenced parent deleted';
EXCEPTION WHEN foreign_key_violation THEN
    RAISE NOTICE 'PASS: 09 default FK rejects referenced parent deletion';
END $$;

SELECT pg_temp.assert_example((SELECT count(*)=1 FROM (
    SELECT c.id FROM customers c LEFT JOIN orders o ON o.customer_id=c.id
    WHERE o.status='PAID' GROUP BY c.id
) result), '10 right WHERE drops zero-order customer');
SELECT pg_temp.assert_example((SELECT array_agg(order_count ORDER BY id)=ARRAY[2::bigint,0::bigint] FROM (
    SELECT c.id, count(o.id) order_count FROM customers c
    LEFT JOIN orders o ON o.customer_id=c.id AND o.status='PAID' GROUP BY c.id
) result), '11 ON filter plus COUNT(id) preserves counts 2 and 0');

CREATE TEMP TABLE members (id integer PRIMARY KEY);
CREATE TEMP TABLE blocked (member_id integer);
INSERT INTO members VALUES (1),(2);
INSERT INTO blocked VALUES (1),(NULL);
SELECT pg_temp.assert_example((SELECT count(*)=0 FROM members WHERE id NOT IN (SELECT member_id FROM blocked)),
    '12 NOT IN containing NULL returns no members');
SELECT pg_temp.assert_example((SELECT array_agg(id)=ARRAY[2] FROM members m
    WHERE NOT EXISTS (SELECT 1 FROM blocked b WHERE b.member_id=m.id)),
    '13 NOT EXISTS returns member2');

CREATE TEMP TABLE comments (id integer PRIMARY KEY, customer_id integer REFERENCES customers(id));
INSERT INTO comments VALUES (1,1),(2,1),(3,1);
SELECT pg_temp.assert_example((SELECT sum(o.amount)=120 FROM orders o JOIN comments c ON c.customer_id=o.customer_id),
    '14 two orders times three comments inflate sum to120');
SELECT pg_temp.assert_example((SELECT sum(DISTINCT o.amount)=20 FROM orders o JOIN comments c ON c.customer_id=o.customer_id),
    '15 DISTINCT amount incorrectly collapses two equal payments to20');
SELECT pg_temp.assert_example((SELECT a.total=40 FROM
    (SELECT customer_id,sum(amount) total FROM orders GROUP BY customer_id) a JOIN
    (SELECT customer_id,count(*) comments FROM comments GROUP BY customer_id) b USING(customer_id)),
    '16 per-customer preaggregation preserves sum40');
SELECT pg_temp.assert_example((SELECT array_agg(id ORDER BY created_at DESC,id DESC)=ARRAY[2,1] FROM orders WHERE customer_id=1),
    '17 unique secondary ordering resolves timestamp ties');
ROLLBACK;
