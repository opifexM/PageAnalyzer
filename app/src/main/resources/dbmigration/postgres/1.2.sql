-- apply alter tables
alter table url_check alter column description type varchar(255) using description::varchar(255);
