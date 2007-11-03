ALTER TABLE USER RENAME USERS;

INSERT INTO CONFIGURATION VALUES('demoMode', 'false');
INSERT INTO CONFIGURATION VALUES('version', '0.7');

ALTER TABLE TIMESHEET_COMMENT MODIFY COMMENT VARCHAR(2048);

CREATE TABLE `CUSTOMER_FOLD_PREFERENCE` (
  `CUSTOMER_ID` int(11) NOT NULL,
  `USER_ID` int(11) NOT NULL,
  `FOLDED` char(1) NOT NULL default 'N',
  PRIMARY KEY  (`CUSTOMER_ID`,`USER_ID`)
) ENGINE=MyISAM;