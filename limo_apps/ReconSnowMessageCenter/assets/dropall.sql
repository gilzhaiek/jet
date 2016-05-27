
DROP TRIGGER IF EXISTS del_group;
DROP TRIGGER IF EXISTS del_category;

DROP TABLE IF EXISTS Messages;
DROP TABLE IF EXISTS MessageCategories;
DROP TABLE IF EXISTS MessageGroups;

DROP VIEW IF EXISTS non_empty_categories;
DROP VIEW IF EXISTS non_empty_groups;
DROP VIEW IF EXISTS unread_messages_by_category;
DROP VIEW IF EXISTS unread_messages_by_group;
DROP VIEW IF EXISTS last_messages_by_category;
DROP VIEW IF EXISTS last_messages_by_group;
DROP VIEW IF EXISTS messages_view;
DROP VIEW IF EXISTS categories_view;
DROP VIEW IF EXISTS groups_view;