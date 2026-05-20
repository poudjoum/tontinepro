package com.tontinepro.tontinepro_backend.infrastructure.storage;

import com.tontinepro.tontinepro_backend.infrastructure.config.MinioConfig;
import io.minio.*;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService {

    private final MinioClient minioClient;

    /**
     * Stocke un fichier dans MinIO et retourne le chemin objet (objectKey).
     *
     * @param bucket  nom du bucket (utiliser MinioConfig.BUCKET_*)
     * @param prefix  préfixe de chemin, ex: "membres/uuid" ou "tontines/uuid"
     * @param fichier le fichier multipart à stocker
     * @return objectKey utilisable pour le téléchargement
     */
    public String stocker(String bucket, String prefix, MultipartFile fichier) {
        try {
            assurerBucket(bucket);

            String ext = "";
            String original = fichier.getOriginalFilename();
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.'));
            }

            String objectKey = prefix + "/" + UUID.randomUUID() + ext;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(fichier.getInputStream(), fichier.getSize(), -1)
                            .contentType(fichier.getContentType() != null
                                    ? fichier.getContentType() : "application/octet-stream")
                            .build()
            );

            return objectKey;

        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Erreur stockage MinIO : " + e.getMessage(), e);
        }
    }

    /**
     * Télécharge un objet depuis MinIO.
     */
    public InputStream telecharger(String bucket, String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Erreur téléchargement MinIO : " + e.getMessage(), e);
        }
    }

    /**
     * Supprime un objet de MinIO.
     */
    public void supprimer(String bucket, String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            log.warn("Erreur suppression MinIO {}/{}: {}", bucket, objectKey, e.getMessage());
        }
    }

    private void assurerBucket(String bucket) throws Exception {
        boolean existe = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build()
        );
        if (!existe) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Bucket MinIO créé : {}", bucket);
        }
    }
}
