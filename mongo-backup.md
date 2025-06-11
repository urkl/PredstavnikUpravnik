mongodbbackup:
image: 'halvves/mongodb-backup-s3:latest'
links:
- mongodb
environment:
- AWS_ACCESS_KEY_ID=myaccesskeyid
- AWS_SECRET_ACCESS_KEY=mysecretaccesskey
- BUCKET=my-s3-bucket
- BACKUP_FOLDER=prod/db/
restart: always