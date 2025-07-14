CHANGE MASTER TO
    MASTER_HOST='mysql-master',
    MASTER_USER='repl_user',
    MASTER_PASSWORD='repl_password',
    MASTER_AUTO_POSITION=1;

START SLAVE;
