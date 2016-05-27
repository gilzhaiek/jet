--categories with at least one message
CREATE VIEW non_empty_categories AS
SELECT MessageCategories.* 
FROM 
	MessageCategories, 
	(SELECT DISTINCT CategoryID FROM Messages) AS CATEGORY_IDS
WHERE CATEGORY_IDS.CategoryID=MessageCategories._id;

--groups with at least one non-empty category
CREATE VIEW non_empty_groups AS
SELECT MessageGroups.* 
FROM 
	MessageGroups, 
	(SELECT DISTINCT GroupID FROM non_empty_categories) AS GROUP_IDS
WHERE GROUP_IDS.GroupID=_id;

--count of unread messages per category with group id
CREATE VIEW unread_messages_by_category AS
SELECT MessageCategories._id, MessageCategories.GroupID, COUNT(UnreadMessages._id) AS UnreadCount
FROM 
	MessageCategories LEFT OUTER JOIN
	(SELECT * FROM Messages WHERE Processed IS NULL OR Processed=0) AS UnreadMessages 
ON  
	MessageCategories._id=UnreadMessages.CategoryID
GROUP BY MessageCategories._id;

--count of unread messages per group
CREATE VIEW unread_messages_by_group AS
SELECT MessageGroups._id, SUM(unread_messages_by_category.UnreadCount) AS UnreadCount
FROM
	MessageGroups LEFT OUTER JOIN unread_messages_by_category
ON
	MessageGroups._id=unread_messages_by_category.GroupID
GROUP BY MessageGroups._id;

--the last message for each category
CREATE VIEW last_messages_by_category AS
SELECT Messages.*
FROM 
	Messages,
	(SELECT _id
	FROM Messages
	GROUP BY CategoryID
	ORDER BY MAX(Timestamp)) AS CAT_LAST_MSG_IDS
WHERE CAT_LAST_MSG_IDS._id=Messages._id;

--the last message for each group
CREATE VIEW last_messages_by_group AS
SELECT *
FROM
	(SELECT *
	FROM last_messages_by_category,MessageCategories
	WHERE last_messages_by_category.CategoryID=MessageCategories._id)
GROUP BY GroupID
ORDER BY MAX(Timestamp);

--all messages joined with groups and categories
CREATE VIEW messages_view AS
SELECT * 
FROM 
	Messages, 
	MessageCategories, 
	MessageGroups
WHERE 
	MessageGroups._id = MessageCategories.GroupID AND 
	MessageCategories._id = Messages.CategoryID;

--all non-empty categories with the last message and count of all unread messages
CREATE VIEW categories_view AS
SELECT non_empty_categories.*, 
last_messages_by_category.Text, last_messages_by_category.Timestamp,
unread_messages_by_category.UnreadCount,
MessageGroups.APK,
MessageGroups.uri AS GroupUri
FROM 
	non_empty_categories,
	unread_messages_by_category,
	last_messages_by_category,
	MessageGroups
WHERE 
	non_empty_categories._id=unread_messages_by_category._id AND 
	non_empty_categories._id=last_messages_by_category.CategoryID AND 
	non_empty_categories.GroupID=MessageGroups._id;

--all non-empty groups with the last message and count of all unread messages
CREATE VIEW groups_view AS
SELECT MessageGroups.*,
LastMessage.Text AS LastMessageText,
LastMessage.Timestamp AS LastMessageTimestamp,
UnreadMessages.UnreadCount
FROM 
	non_empty_groups AS MessageGroups,
	last_messages_by_group AS LastMessage,
	unread_messages_by_group AS UnreadMessages
WHERE 
	MessageGroups._id=LastMessage.GroupID AND
	MessageGroups._id=UnreadMessages._id
GROUP BY MessageGroups._id;

