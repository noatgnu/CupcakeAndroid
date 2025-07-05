package info.proteo.cupcake.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

object RoomMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
        }
    }

    fun createAllTables(database: SupportSQLiteDatabase) {
        // User related tables
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS user_basic (
                id INTEGER PRIMARY KEY NOT NULL,
                username TEXT NOT NULL,
                first_name TEXT,
                last_name TEXT,
                email TEXT,
                avatar TEXT
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS user (
                id INTEGER PRIMARY KEY NOT NULL,
                username TEXT NOT NULL UNIQUE,
                first_name TEXT,
                last_name TEXT,
                email TEXT,
                password TEXT,
                phone TEXT,
                is_active INTEGER NOT NULL DEFAULT 1,
                date_joined TEXT,
                last_login TEXT,
                lab_group_id INTEGER,
                avatar TEXT
            )
        """)

        // Lab Group tables
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS lab_group_basic (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS lab_group (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                description TEXT,
                default_storage INTEGER,
                is_professional INTEGER NOT NULL DEFAULT 0,
                service_storage INTEGER,
                remote_id INTEGER,
                remote_host INTEGER,
                created_at TEXT,
                updated_at TEXT,
                FOREIGN KEY (default_storage) REFERENCES storage_object(id) ON DELETE SET NULL,
                FOREIGN KEY (service_storage) REFERENCES storage_object(id) ON DELETE SET NULL,
                FOREIGN KEY (remote_host) REFERENCES remote_host(id) ON DELETE SET NULL
            )
        """)

        // Contact tables
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS external_contact_details (
                id INTEGER PRIMARY KEY NOT NULL,
                phone TEXT,
                email TEXT,
                address TEXT,
                other_details TEXT
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS external_contact (
                id INTEGER PRIMARY KEY NOT NULL,
                contact_value TEXT
            )
        """)

        // Support information
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS support_information (
                id INTEGER PRIMARY KEY NOT NULL,
                vendor_name TEXT,
                manufacturer_name TEXT,
                serial_number TEXT,
                maintenance_frequency_days INTEGER,
                location_id INTEGER,
                warranty_start_date TEXT,
                warranty_end_date TEXT,
                created_at TEXT,
                updated_at TEXT,
                FOREIGN KEY (location_id) REFERENCES storage_object(id) ON DELETE SET NULL
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS support_information_vendor_contact (
                support_information_id INTEGER NOT NULL,
                contact_id INTEGER NOT NULL,
                PRIMARY KEY(support_information_id, contact_id),
                FOREIGN KEY (support_information_id) REFERENCES support_information(id) ON DELETE CASCADE,
                FOREIGN KEY (contact_id) REFERENCES external_contact(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS support_information_manufacturer_contact (
                support_information_id INTEGER NOT NULL,
                contact_id INTEGER NOT NULL,
                PRIMARY KEY(support_information_id, contact_id),
                FOREIGN KEY (support_information_id) REFERENCES support_information(id) ON DELETE CASCADE,
                FOREIGN KEY (contact_id) REFERENCES external_contact(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS protocol_model (
                id INTEGER PRIMARY KEY NOT NULL,
                protocol_id INTEGER,
                protocol_created_on TEXT,
                protocol_doi TEXT,
                protocol_title TEXT NOT NULL,
                protocol_description TEXT,
                protocol_url TEXT,
                protocol_version_uri TEXT,
                enabled INTEGER NOT NULL DEFAULT 1,
                complexity_rating REAL DEFAULT 0,
                duration_rating REAL DEFAULT 0,
                user INTEGER,
                remote_id INTEGER,
                model_hash TEXT,
                remote_host INTEGER,
                created_at TEXT,
                updated_at TEXT,
                FOREIGN KEY (user) REFERENCES user(id) ON DELETE SET NULL
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS protocol_editor_cross_ref (
                protocolId INTEGER NOT NULL,
                userId INTEGER NOT NULL,
                PRIMARY KEY(protocolId, userId),
                FOREIGN KEY(protocolId) REFERENCES protocol_model(id) ON DELETE CASCADE,
                FOREIGN KEY(userId) REFERENCES user(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS protocol_viewer_cross_ref (
                protocolId INTEGER NOT NULL,
                userId INTEGER NOT NULL,
                PRIMARY KEY(protocolId, userId),
                FOREIGN KEY(protocolId) REFERENCES protocol_model(id) ON DELETE CASCADE,
                FOREIGN KEY(userId) REFERENCES user(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS protocol_section (
                id INTEGER PRIMARY KEY NOT NULL,
                protocol INTEGER NOT NULL,
                section_description TEXT,
                section_duration LONG,
                created_at TEXT,
                updated_at TEXT,
                remote_id LONG,
                remote_host INTEGER,
                FOREIGN KEY (protocol) REFERENCES protocol_model(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS protocol_step (
                id INTEGER PRIMARY KEY NOT NULL,
                protocol INTEGER NOT NULL,
                step_id INTEGER,
                step_description TEXT NOT NULL,
                step_section INTEGER,
                step_duration INTEGER,
                previous_step INTEGER,
                original INTEGER NOT NULL DEFAULT 1,
                branch_from INTEGER,
                remote_id INTEGER,
                created_at TEXT,
                updated_at TEXT,
                remote_host INTEGER,
                FOREIGN KEY (protocol) REFERENCES protocol_model(id) ON DELETE CASCADE,
                FOREIGN KEY (step_section) REFERENCES protocol_section(id) ON DELETE SET NULL,
                FOREIGN KEY (previous_step) REFERENCES protocol_step(id) ON DELETE SET NULL
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS protocol_step_next_relation (
                from_step INTEGER NOT NULL,
                to_step INTEGER NOT NULL,
                PRIMARY KEY(from_step, to_step),
                FOREIGN KEY (from_step) REFERENCES protocol_step(id) ON DELETE CASCADE,
                FOREIGN KEY (to_step) REFERENCES protocol_step(id) ON DELETE CASCADE
            )
        """)



        // Annotation tables
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS annotation (
                id INTEGER PRIMARY KEY NOT NULL,
                step INTEGER,
                session INTEGER,
                annotation TEXT,
                file TEXT,
                created_at TEXT,
                updated_at TEXT,
                annotation_type TEXT,
                transcribed INTEGER,
                transcription TEXT,
                language TEXT,
                translation TEXT,
                scratched INTEGER,
                annotation_name TEXT,
                summary TEXT,
                fixed INTEGER,
                user_id INTEGER,
                stored_reagent INTEGER,
                folder_id INTEGER,
                FOREIGN KEY (step) REFERENCES protocol_step(id) ON DELETE CASCADE,
                FOREIGN KEY (session) REFERENCES session(id) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL,
                FOREIGN KEY (stored_reagent) REFERENCES stored_reagent(id) ON DELETE CASCADE,
                FOREIGN KEY (folder_id) REFERENCES annotation_folder(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS annotation_folder (
                id INTEGER PRIMARY KEY NOT NULL,
                folder_name TEXT NOT NULL,
                created_at TEXT,
                updated_at TEXT,
                parent_folder INTEGER,
                session INTEGER,
                instrument INTEGER,
                stored_reagent INTEGER,
                is_shared_document_folder INTEGER NOT NULL DEFAULT 0,
                owner_id INTEGER,
                remote_id INTEGER,
                remote_host INTEGER,
                FOREIGN KEY (parent_folder) REFERENCES annotation_folder(id) ON DELETE CASCADE,
                FOREIGN KEY (session) REFERENCES session(id) ON DELETE CASCADE,
                FOREIGN KEY (instrument) REFERENCES instrument(id) ON DELETE CASCADE,
                FOREIGN KEY (stored_reagent) REFERENCES stored_reagent(id) ON DELETE CASCADE,
                FOREIGN KEY (owner_id) REFERENCES user(id) ON DELETE SET NULL
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS annotation_folder_path (
                id INTEGER PRIMARY KEY NOT NULL,
                folder_id INTEGER NOT NULL,
                path TEXT NOT NULL,
                FOREIGN KEY (folder_id) REFERENCES annotation_folder(id) ON DELETE CASCADE
            )
        """)

        // Step variations
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS step_variation (
                id INTEGER PRIMARY KEY NOT NULL,
                step INTEGER NOT NULL,
                variation_description TEXT,
                variation_duration INTEGER,
                created_at TEXT,
                updated_at TEXT,
                remote_id INTEGER,
                remote_host INTEGER,
                FOREIGN KEY (step) REFERENCES protocol_step(id) ON DELETE CASCADE
            )
        """)

        // Session
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS session (
                id INTEGER PRIMARY KEY NOT NULL,
                unique_id TEXT NOT NULL UNIQUE,
                protocol INTEGER NOT NULL,
                user INTEGER,
                started_at TEXT,
                completed_at TEXT,
                notes TEXT,
                FOREIGN KEY (protocol) REFERENCES protocol_model(id) ON DELETE CASCADE,
                FOREIGN KEY (user) REFERENCES user(id) ON DELETE SET NULL
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS recent_session
                (id INTEGER PRIMARY KEY NOT NULL,
                session_id INTEGER NOT NULL,
                session_unique_id TEXT NOT NULL,
                session_name TEXT,
                protocol_id INTEGER NOT NULL,
                user_id INTEGER NOT NULL,
                protocol_name TEXT,
                last_accessed TEXT NOT NULL,
                step_id INTEGER,
                FOREIGN KEY (session_id) REFERENCES session(id) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
                FOREIGN KEY (protocol_id) REFERENCES protocol_model(id) ON DELETE CASCADE,
                FOREIGN KEY (step_id) REFERENCES protocol_step(id) ON DELETE SET NULL)
        """.trimIndent())


        // Time tracking
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS time_keeper (
                id INTEGER PRIMARY KEY NOT NULL,
                session INTEGER NOT NULL,
                step INTEGER NOT NULL,
                start_time TEXT NOT NULL,
                end_time TEXT,
                current_duration INTEGER NOT NULL DEFAULT 0,
                started INTEGER NOT NULL DEFAULT 0,
                user_id INTEGER,
                FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL,
                FOREIGN KEY (session) REFERENCES session(id) ON DELETE CASCADE,
                FOREIGN KEY (step) REFERENCES protocol_step(id) ON DELETE CASCADE
            )
        """)

        // Protocol ratings
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS protocol_rating (
                id INTEGER PRIMARY KEY NOT NULL,
                protocol INTEGER NOT NULL,
                user INTEGER NOT NULL,
                rating INTEGER NOT NULL,
                comment TEXT,
                created_at TEXT,
                FOREIGN KEY (protocol) REFERENCES protocol_model(id) ON DELETE CASCADE,
                FOREIGN KEY (user) REFERENCES user(id) ON DELETE CASCADE
            )
        """)

        // Reagent related tables
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS reagent (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                description TEXT,
                cas_number TEXT,
                catalog_number TEXT,
                vendor TEXT,
                created_at TEXT,
                updated_at TEXT
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS protocol_reagent (
                id INTEGER PRIMARY KEY NOT NULL,
                protocol INTEGER NOT NULL,
                reagent INTEGER NOT NULL,
                quantity REAL,
                unit TEXT,
                FOREIGN KEY (protocol) REFERENCES protocol_model(id) ON DELETE CASCADE,
                FOREIGN KEY (reagent) REFERENCES reagent(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS step_reagent (
                id INTEGER PRIMARY KEY NOT NULL,
                step INTEGER NOT NULL,
                reagent INTEGER NOT NULL,
                quantity REAL,
                unit TEXT,
                FOREIGN KEY (step) REFERENCES protocol_step(id) ON DELETE CASCADE,
                FOREIGN KEY (reagent) REFERENCES reagent(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS stored_reagent (
                id INTEGER PRIMARY KEY NOT NULL,
                reagent_id INTEGER NOT NULL,
                storage_object_id INTEGER NOT NULL,
                quantity REAL NOT NULL,
                notes TEXT,
                user_id INTEGER NOT NULL,
                created_at TEXT,
                updated_at TEXT,
                current_quantity REAL NOT NULL,
                png_base64 TEXT,
                barcode TEXT,
                shareable INTEGER NOT NULL DEFAULT 1,
                expiration_date TEXT,
                created_by_session TEXT,
                notify_on_low_stock INTEGER NOT NULL DEFAULT 0,
                last_notification_sent TEXT,
                low_stock_threshold REAL,
                notify_days_before_expiry INTEGER,
                notify_on_expiry INTEGER NOT NULL DEFAULT 0,
                last_expiry_notification_sent TEXT,
                subscriber_count INTEGER NOT NULL DEFAULT 0,
                access_all INTEGER NOT NULL DEFAULT 0,
                created_by_project INTEGER,
                created_by_protocol INTEGER,
                created_by_step INTEGER,
                remote_id INTEGER,
                remote_host INTEGER,
                FOREIGN KEY (reagent_id) REFERENCES reagent(id) ON DELETE CASCADE,
                FOREIGN KEY (storage_object_id) REFERENCES storage_object(id) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
                FOREIGN KEY (created_by_project) REFERENCES project(id) ON DELETE SET NULL,
                FOREIGN KEY (created_by_protocol) REFERENCES protocol_model(id) ON DELETE SET NULL,
                FOREIGN KEY (created_by_step) REFERENCES protocol_step(id) ON DELETE SET NULL,
                FOREIGN KEY (remote_host) REFERENCES remote_host(id) ON DELETE SET NULL
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS stored_reagent_access_user (
                storedReagentId INTEGER NOT NULL,
                userId INTEGER NOT NULL,
                PRIMARY KEY(storedReagentId, userId),
                FOREIGN KEY (storedReagentId) REFERENCES stored_reagent(id) ON DELETE CASCADE,
                FOREIGN KEY (userId) REFERENCES user(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS stored_reagent_access_lab_group (
                storedReagentId INTEGER NOT NULL,
                labGroupId INTEGER NOT NULL,
                PRIMARY KEY(storedReagentId, labGroupId),
                FOREIGN KEY (storedReagentId) REFERENCES stored_reagent(id) ON DELETE CASCADE,
                FOREIGN KEY (labGroupId) REFERENCES lab_group(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS reagent_subscription (
                id INTEGER PRIMARY KEY NOT NULL,
                user_id INTEGER NOT NULL,
                stored_reagent INTEGER NOT NULL,
                notify_on_low_stock INTEGER NOT NULL DEFAULT 1,
                notify_on_expiry INTEGER NOT NULL DEFAULT 1,
                created_at TEXT,
                FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
                FOREIGN KEY (stored_reagent) REFERENCES stored_reagent(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS reagent_action (
                id INTEGER PRIMARY KEY NOT NULL,
                reagent INTEGER NOT NULL,
                user TEXT NOT NULL,
                action_type TEXT NOT NULL,
                quantity_change REAL,
                unit TEXT,
                notes TEXT,
                created_at TEXT,
                FOREIGN KEY (reagent) REFERENCES stored_reagent(id) ON DELETE CASCADE
            )
        """)

        // Tags
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS tag (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL UNIQUE,
                created_at TEXT
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS step_tag (
                id INTEGER PRIMARY KEY NOT NULL,
                step INTEGER NOT NULL,
                tag INTEGER NOT NULL,
                FOREIGN KEY (step) REFERENCES protocol_step(id) ON DELETE CASCADE,
                FOREIGN KEY (tag) REFERENCES tag(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS protocol_tag (
                id INTEGER PRIMARY KEY NOT NULL,
                protocol INTEGER NOT NULL,
                tag INTEGER NOT NULL,
                FOREIGN KEY (protocol) REFERENCES protocol_model(id) ON DELETE CASCADE,
                FOREIGN KEY (tag) REFERENCES tag(id) ON DELETE CASCADE
            )
        """)

        // Project
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS project (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                description TEXT,
                owner TEXT NOT NULL,
                created_at TEXT,
                updated_at TEXT,
                FOREIGN KEY (owner) REFERENCES user(username)
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS instrument (
                id INTEGER PRIMARY KEY NOT NULL,
                max_days_ahead_pre_approval INTEGER,
                max_days_within_usage_pre_approval INTEGER,
                instrument_name TEXT NOT NULL,
                instrument_description TEXT,
                created_at TEXT,
                updated_at TEXT,
                enabled INTEGER NOT NULL DEFAULT 1,
                image TEXT,
                days_before_maintenance_notification INTEGER DEFAULT 14,
                days_before_warranty_notification INTEGER DEFAULT 30,
                last_maintenance_notification_sent TEXT,
                last_warranty_notification_sent TEXT,
                accepts_bookings INTEGER
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS instrument_usage (
                id INTEGER PRIMARY KEY NOT NULL,
                instrument INTEGER NOT NULL,
                user INTEGER NOT NULL,
                start_time TEXT NOT NULL,
                end_time TEXT,
                notes TEXT,
                FOREIGN KEY (instrument) REFERENCES instrument(id) ON DELETE CASCADE,
                FOREIGN KEY (user) REFERENCES user(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS maintenance_log (
                id INTEGER PRIMARY KEY NOT NULL,
                instrument INTEGER NOT NULL,
                maintenance_type TEXT NOT NULL,
                description TEXT,
                performed_at TEXT,
                created_by INTEGER,
                is_template INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (instrument) REFERENCES instrument(id) ON DELETE CASCADE,
                FOREIGN KEY (created_by) REFERENCES user(id) ON DELETE SET NULL
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS instrument_job (
                id INTEGER PRIMARY KEY NOT NULL,
                instrument INTEGER NOT NULL,
                user_id INTEGER NOT NULL,
                status TEXT NOT NULL,
                job_type TEXT NOT NULL,
                created_at TEXT,
                started_at TEXT,
                completed_at TEXT,
                priority INTEGER,
                notes TEXT,
                FOREIGN KEY (instrument) REFERENCES instrument(id) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
            )
        """)

        // Storage
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS storage_object (
                id INTEGER PRIMARY KEY NOT NULL,
                object_name TEXT NOT NULL,
                object_type TEXT,
                object_description TEXT,
                created_at TEXT,
                updated_at TEXT,
                can_delete INTEGER NOT NULL DEFAULT 0,
                stored_at INTEGER,
                png_base64 TEXT,
                user TEXT,
                remote_id INTEGER,
                remote_host INTEGER,
                FOREIGN KEY (stored_at) REFERENCES storage_object(id) ON DELETE CASCADE,
                FOREIGN KEY (remote_host) REFERENCES remote_host(id) ON DELETE SET NULL
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS storage_object_access_lab_group (
                storageObjectId INTEGER NOT NULL,
                labGroupId INTEGER NOT NULL,
                PRIMARY KEY(storageObjectId, labGroupId),
                FOREIGN KEY (storageObjectId) REFERENCES storage_object(id) ON DELETE CASCADE,
                FOREIGN KEY (labGroupId) REFERENCES lab_group(id) ON DELETE CASCADE
            )
        """)

        // Metadata
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS metadata_column (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                column_type TEXT NOT NULL,
                stored_reagent INTEGER,
                order_index INTEGER,
                required INTEGER NOT NULL DEFAULT 0,
                hidden INTEGER NOT NULL DEFAULT 0,
                options TEXT,
                default_value TEXT,
                created_at TEXT,
                updated_at TEXT,
                FOREIGN KEY (stored_reagent) REFERENCES stored_reagent(id) ON DELETE SET NULL
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS favourite_metadata_option (
                id INTEGER PRIMARY KEY NOT NULL,
                user INTEGER NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                value TEXT NOT NULL,
                display_value TEXT,
                service_lab_group INTEGER,
                lab_group INTEGER,
                preset INTEGER,
                created_at TEXT,
                updated_at TEXT,
                is_global INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (user) REFERENCES user(id) ON DELETE CASCADE,
                FOREIGN KEY (service_lab_group) REFERENCES lab_group(id) ON DELETE CASCADE,
                FOREIGN KEY (lab_group) REFERENCES lab_group(id) ON DELETE CASCADE,
                FOREIGN KEY (preset) REFERENCES preset(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS preset (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                user INTEGER NOT NULL,
                created_at TEXT,
                updated_at TEXT,
                FOREIGN KEY (user) REFERENCES user(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS metadata_table_template (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT,
                user INTEGER,
                created_at TEXT,
                updated_at TEXT,
                hidden_user_columns INTEGER,
                hidden_staff_columns INTEGER,
                service_lab_group INTEGER,
                lab_group INTEGER,
                field_mask_mapping TEXT,
                enabled INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY (user) REFERENCES user(id) ON DELETE SET NULL,
                FOREIGN KEY (service_lab_group) REFERENCES lab_group(id) ON DELETE SET NULL,
                FOREIGN KEY (lab_group) REFERENCES lab_group(id) ON DELETE SET NULL
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS metadata_table_template_user_column (
                templateId INTEGER NOT NULL,
                columnId INTEGER NOT NULL,
                PRIMARY KEY(templateId, columnId),
                FOREIGN KEY (templateId) REFERENCES metadata_table_template(id) ON DELETE CASCADE,
                FOREIGN KEY (columnId) REFERENCES metadata_column(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS metadata_table_template_staff_column (
                templateId INTEGER NOT NULL,
                columnId INTEGER NOT NULL,
                PRIMARY KEY(templateId, columnId),
                FOREIGN KEY (templateId) REFERENCES metadata_table_template(id) ON DELETE CASCADE,
                FOREIGN KEY (columnId) REFERENCES metadata_column(id) ON DELETE CASCADE
            )
        """)

        // Messaging
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS message (
                id INTEGER PRIMARY KEY NOT NULL,
                sender_id INTEGER,
                content TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT,
                message_type TEXT NOT NULL,
                priority TEXT,
                thread_id INTEGER NOT NULL,
                expires_at TEXT,
                project INTEGER,
                protocol INTEGER,
                session INTEGER,
                instrument INTEGER,
                instrument_job INTEGER,
                stored_reagent INTEGER,
                FOREIGN KEY (sender_id) REFERENCES user(id) ON DELETE SET NULL,
                FOREIGN KEY (thread_id) REFERENCES message_thread(id) ON DELETE CASCADE,
                FOREIGN KEY (project) REFERENCES project(id) ON DELETE SET NULL,
                FOREIGN KEY (protocol) REFERENCES protocol_model(id) ON DELETE SET NULL,
                FOREIGN KEY (session) REFERENCES session(id) ON DELETE SET NULL,
                FOREIGN KEY (instrument) REFERENCES instrument(id) ON DELETE SET NULL,
                FOREIGN KEY (instrument_job) REFERENCES instrument_job(id) ON DELETE SET NULL,
                FOREIGN KEY (stored_reagent) REFERENCES stored_reagent(id) ON DELETE SET NULL
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS message_recipient (
                id INTEGER PRIMARY KEY NOT NULL,
                message_id INTEGER NOT NULL,
                user_id INTEGER NOT NULL,
                is_read INTEGER NOT NULL DEFAULT 0,
                read_at TEXT,
                is_archived INTEGER NOT NULL DEFAULT 0,
                is_deleted INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (message_id) REFERENCES message(id) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS message_attachment (
                id INTEGER PRIMARY KEY NOT NULL,
                message_id INTEGER NOT NULL,
                file TEXT NOT NULL,
                file_name TEXT NOT NULL,
                file_size INTEGER,
                content_type TEXT,
                created_at TEXT,
                FOREIGN KEY (message_id) REFERENCES message(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS message_thread (
                id INTEGER PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT,
                is_system_thread INTEGER NOT NULL DEFAULT 0,
                creator_id INTEGER,
                lab_group_id INTEGER,
                FOREIGN KEY (creator_id) REFERENCES user(id) ON DELETE SET NULL,
                FOREIGN KEY (lab_group_id) REFERENCES lab_group(id) ON DELETE SET NULL
            )
        """)

        // Vocabulary and reference data tables
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS ms_unique_vocabularies (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                accession TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                short_name TEXT,
                uri TEXT,
                definition TEXT,
                synonyms TEXT,
                term_type TEXT NOT NULL,
                is_obsolete INTEGER NOT NULL DEFAULT 0,
                created_at TEXT,
                updated_at TEXT
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS human_disease (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                accession TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                definition TEXT,
                synonyms TEXT,
                created_at TEXT,
                updated_at TEXT
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS tissue (
                id INTEGER PRIMARY KEY NOT NULL,
                identifier TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                short_name TEXT,
                definition TEXT,
                created_at TEXT,
                updated_at TEXT
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS subcellular_location (
                id INTEGER PRIMARY KEY NOT NULL,
                location_identifier TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                definition TEXT,
                synonyms TEXT,
                created_at TEXT,
                updated_at TEXT
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS species (
                id INTEGER PRIMARY KEY NOT NULL,
                code TEXT NOT NULL UNIQUE,
                scientific_name TEXT NOT NULL,
                common_name TEXT,
                taxonomy_id INTEGER,
                created_at TEXT,
                updated_at TEXT
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS unimod (
                id INTEGER PRIMARY KEY NOT NULL,
                accession TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                mono_mass REAL,
                avg_mass REAL,
                composition TEXT,
                created_at TEXT,
                updated_at TEXT
            )
        """)

        // Cache table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS limit_offset_cache (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                cacheKey TEXT NOT NULL UNIQUE,
                dataJson TEXT NOT NULL,
                lastUpdated INTEGER NOT NULL
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `user_preferences` (
                `user_id` TEXT NOT NULL,
                `hostname` TEXT NOT NULL,
                `auth_token` TEXT,
                `session_token` TEXT,
                `last_login_timestamp` INTEGER NOT NULL DEFAULT 0,
                `remember_credentials` INTEGER NOT NULL DEFAULT 0,
                `theme` TEXT NOT NULL DEFAULT 'system',
                `notifications_enabled` INTEGER NOT NULL DEFAULT 1,
                `sync_frequency` INTEGER NOT NULL DEFAULT 15,
                `sync_on_wifi_only` INTEGER NOT NULL DEFAULT 1,
                `last_sync_timestamp` INTEGER NOT NULL DEFAULT 0,
                `is_active` INTEGER NOT NULL DEFAULT 0,
                `allow_overlap_bookings` INTEGER NOT NULL DEFAULT 0,
                `use_coturn` INTEGER NOT NULL DEFAULT 0,
                `use_llm` INTEGER NOT NULL DEFAULT 0,
                `use_ocr` INTEGER NOT NULL DEFAULT 0,
                `use_whisper` INTEGER NOT NULL DEFAULT 0,
                `default_service_lab_group` TEXT NOT NULL DEFAULT 'MS Facility',
                `can_send_email` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`userId`, `hostname`)
            )
        """)

        // System tables
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS remote_host (
                id INTEGER PRIMARY KEY NOT NULL,
                host_name TEXT NOT NULL,
                host_url TEXT NOT NULL,
                host_token TEXT,
                created_at TEXT,
                updated_at TEXT
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS site_settings (
                id INTEGER PRIMARY KEY NOT NULL,
                is_active INTEGER NOT NULL DEFAULT 0,
                site_name TEXT,
                site_tagline TEXT,
                logo TEXT,
                favicon TEXT,
                banner_enabled INTEGER NOT NULL DEFAULT 0,
                banner_text TEXT,
                banner_color TEXT,
                banner_text_color TEXT,
                banner_dismissible INTEGER NOT NULL DEFAULT 1,
                primary_color TEXT,
                secondary_color TEXT,
                footer_text TEXT,
                created_at TEXT,
                updated_at TEXT,
                updated_by INTEGER,
                allow_import_protocols INTEGER NOT NULL DEFAULT 1,
                allow_import_reagents INTEGER NOT NULL DEFAULT 1,
                allow_import_storage_objects INTEGER NOT NULL DEFAULT 1,
                allow_import_instruments INTEGER NOT NULL DEFAULT 1,
                allow_import_users INTEGER NOT NULL DEFAULT 1,
                allow_import_lab_groups INTEGER NOT NULL DEFAULT 1,
                allow_import_sessions INTEGER NOT NULL DEFAULT 1,
                allow_import_projects INTEGER NOT NULL DEFAULT 1,
                allow_import_annotations INTEGER NOT NULL DEFAULT 1,
                allow_import_metadata INTEGER NOT NULL DEFAULT 1,
                staff_only_import_override INTEGER NOT NULL DEFAULT 0,
                import_archive_size_limit_mb INTEGER NOT NULL DEFAULT 500,
                FOREIGN KEY (updated_by) REFERENCES user(id) ON DELETE SET NULL
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS backup_log (
                id INTEGER PRIMARY KEY NOT NULL,
                backup_type TEXT NOT NULL,
                status TEXT NOT NULL,
                started_at TEXT,
                completed_at TEXT,
                duration_seconds INTEGER,
                backup_file_path TEXT,
                file_size_bytes INTEGER,
                error_message TEXT,
                success_message TEXT,
                triggered_by TEXT,
                container_id TEXT
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS document_permission (
                id INTEGER PRIMARY KEY NOT NULL,
                annotation_id INTEGER,
                folder_id INTEGER,
                user_id INTEGER,
                lab_group_id INTEGER,
                can_view INTEGER NOT NULL DEFAULT 0,
                can_download INTEGER NOT NULL DEFAULT 0,
                can_comment INTEGER NOT NULL DEFAULT 0,
                can_edit INTEGER NOT NULL DEFAULT 0,
                can_share INTEGER NOT NULL DEFAULT 0,
                can_delete INTEGER NOT NULL DEFAULT 0,
                shared_by INTEGER,
                shared_at TEXT,
                expires_at TEXT,
                last_accessed TEXT,
                access_count INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (annotation_id) REFERENCES annotation(id) ON DELETE CASCADE,
                FOREIGN KEY (folder_id) REFERENCES annotation_folder(id) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
                FOREIGN KEY (lab_group_id) REFERENCES lab_group(id) ON DELETE CASCADE,
                FOREIGN KEY (shared_by) REFERENCES user(id) ON DELETE SET NULL
            )
        """)

        // WebRTC Communication tables
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS webrtc_session (
                id INTEGER PRIMARY KEY NOT NULL,
                session_id TEXT NOT NULL,
                is_active INTEGER NOT NULL DEFAULT 0,
                created_at TEXT,
                updated_at TEXT
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS webrtc_user_channel (
                id INTEGER PRIMARY KEY NOT NULL,
                user_id INTEGER NOT NULL,
                channel_name TEXT NOT NULL,
                channel_type TEXT NOT NULL,
                created_at TEXT,
                FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS webrtc_user_offer (
                id INTEGER PRIMARY KEY NOT NULL,
                user_id INTEGER NOT NULL,
                offer_data TEXT NOT NULL,
                id_type TEXT NOT NULL,
                created_at TEXT,
                FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
            )
        """)

        // Import Tracker tables
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS import_tracker (
                id INTEGER PRIMARY KEY NOT NULL,
                import_type TEXT,
                import_status TEXT,
                created_at TEXT,
                updated_at TEXT,
                created_by INTEGER,
                import_name TEXT,
                import_description TEXT,
                total_objects INTEGER,
                processed_objects INTEGER,
                created_objects INTEGER,
                updated_objects INTEGER,
                failed_objects INTEGER,
                error_log TEXT,
                import_metadata TEXT,
                file_size_bytes INTEGER,
                lab_group INTEGER,
                FOREIGN KEY (created_by) REFERENCES user(id) ON DELETE SET NULL,
                FOREIGN KEY (lab_group) REFERENCES lab_group(id) ON DELETE SET NULL
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS imported_object (
                id INTEGER PRIMARY KEY NOT NULL,
                import_tracker INTEGER NOT NULL,
                object_type TEXT NOT NULL,
                object_id INTEGER NOT NULL,
                action_type TEXT NOT NULL,
                created_at TEXT,
                object_data TEXT,
                FOREIGN KEY (import_tracker) REFERENCES import_tracker(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS imported_file (
                id INTEGER PRIMARY KEY NOT NULL,
                import_tracker INTEGER NOT NULL,
                file_path TEXT NOT NULL,
                original_filename TEXT,
                file_size_bytes INTEGER,
                file_hash TEXT,
                created_at TEXT,
                FOREIGN KEY (import_tracker) REFERENCES import_tracker(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS imported_relationship (
                id INTEGER PRIMARY KEY NOT NULL,
                import_tracker INTEGER NOT NULL,
                relationship_type TEXT NOT NULL,
                parent_model TEXT NOT NULL,
                parent_id INTEGER NOT NULL,
                child_model TEXT NOT NULL,
                child_id INTEGER NOT NULL,
                created_at TEXT,
                FOREIGN KEY (import_tracker) REFERENCES import_tracker(id) ON DELETE CASCADE
            )
        """)

        // Create indexes for better query performance
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_protocol_step_protocol ON protocol_step(protocol)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_protocol_section_protocol ON protocol_section(protocol)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_annotation_step ON annotation(step)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_annotation_session ON annotation(session)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_step_variation_step ON step_variation(step)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_time_keeper_session ON time_keeper(session)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_protocol_rating_protocol ON protocol_rating(protocol)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_protocol_reagent_protocol ON protocol_reagent(protocol)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_step_reagent_step ON step_reagent(step)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_step_tag_step ON step_tag(step)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_protocol_tag_protocol ON protocol_tag(protocol)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_message_thread ON message(thread_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_message_recipient_message ON message_recipient(message_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_support_info_vendor_contact ON support_information_vendor_contact(support_information_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_support_info_manufacturer_contact ON support_information_manufacturer_contact(support_information_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_protocol_step_next_from ON protocol_step_next_relation(from_step)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_protocol_step_next_to ON protocol_step_next_relation(to_step)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_recent_session_session ON recent_session(session_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_recent_session_user ON recent_session(user_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_recent_session_protocol ON recent_session(protocol_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_recent_session_step ON recent_session(step_id)")
        
        // Indexes for new tables
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_document_permission_annotation ON document_permission(annotation_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_document_permission_folder ON document_permission(folder_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_document_permission_user ON document_permission(user_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_document_permission_lab_group ON document_permission(lab_group_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_webrtc_session_active ON webrtc_session(is_active)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_webrtc_user_channel_user ON webrtc_user_channel(user_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_webrtc_user_offer_user ON webrtc_user_offer(user_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_backup_log_status ON backup_log(status)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_backup_log_started_at ON backup_log(started_at)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_annotation_folder_owner ON annotation_folder(owner_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_annotation_folder_shared ON annotation_folder(is_shared_document_folder)")
        
        // Indexes for new tables
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_storage_object_access_lab_group_storage ON storage_object_access_lab_group(storageObjectId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_storage_object_access_lab_group_lab ON storage_object_access_lab_group(labGroupId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_stored_reagent_access_user_reagent ON stored_reagent_access_user(storedReagentId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_stored_reagent_access_user_user ON stored_reagent_access_user(userId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_stored_reagent_access_lab_group_reagent ON stored_reagent_access_lab_group(storedReagentId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_stored_reagent_access_lab_group_lab ON stored_reagent_access_lab_group(labGroupId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_metadata_template_user_column_template ON metadata_table_template_user_column(templateId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_metadata_template_user_column_column ON metadata_table_template_user_column(columnId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_metadata_template_staff_column_template ON metadata_table_template_staff_column(templateId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_metadata_template_staff_column_column ON metadata_table_template_staff_column(columnId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_import_tracker_status ON import_tracker(import_status)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_import_tracker_created_by ON import_tracker(created_by)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_imported_object_tracker ON imported_object(import_tracker)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_imported_object_type_id ON imported_object(object_type, object_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_imported_file_tracker ON imported_file(import_tracker)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_imported_relationship_tracker ON imported_relationship(import_tracker)")
    }
}