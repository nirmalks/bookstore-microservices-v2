UPDATE Book
SET stock = 500
WHERE id = 1
  AND stock < 500;

UPDATE Book
SET stock = 500
WHERE id = 2
  AND stock < 500;
