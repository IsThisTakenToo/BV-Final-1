package com.spotvault.app

import java.io.File

/**
 * Hard-purge Recently Deleted rows older than [cutoff], deleting cover + extra photo files first
 * in paged batches so a multi-year trash pile cannot allocate one giant path list.
 *
 * @return true if any rows or files were processed (triggers tag usage recompute at call sites).
 */
suspend fun purgeExpiredDeletedSpots(dao: LocationDao, cutoff: Long): Boolean {
    var touched = false
    var afterCoverId = 0
    while (true) {
        val page = dao.getDeletedCoverPathsOlderThanPage(cutoff, afterCoverId, 500)
        if (page.isEmpty()) break
        page.forEach { row ->
            if (row.imagePath.isNotEmpty()) {
                runCatching { File(row.imagePath).delete() }
            }
        }
        afterCoverId = page.last().id
        touched = true
    }
    var afterPhotoId = 0
    while (true) {
        val page = dao.getExtraPhotoPathsDeletedOlderThanPage(cutoff, afterPhotoId, 500)
        if (page.isEmpty()) break
        page.forEach { row ->
            runCatching { File(row.path).delete() }
        }
        afterPhotoId = page.last().id
        touched = true
    }
    while (true) {
        val deleted = dao.purgeDeletedOlderThanBatch(cutoff, limit = 200)
        if (deleted == 0) break
        touched = true
    }
    return touched
}
