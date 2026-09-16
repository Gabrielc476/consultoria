import boto3
from botocore.client import BaseClient

from infrastructure.config.settings import Settings


class S3DocumentClient:
    def __init__(self, settings: Settings) -> None:
        self._bucket_default = settings.minio_documents_bucket
        self._client: BaseClient = boto3.client(
            "s3",
            endpoint_url=settings.minio_endpoint,
            aws_access_key_id=settings.minio_access_key,
            aws_secret_access_key=settings.minio_secret_key,
            region_name=settings.minio_region,
        )

    def download_bytes(self, bucket: str, key: str) -> bytes:
        response = self._client.get_object(Bucket=bucket, Key=key)
        body = response["Body"].read()
        if not body:
            raise FileNotFoundError(f"Objeto S3 vazio: s3://{bucket}/{key}")
        return body
