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
                address TEXT,
                created_at TEXT,
                updated_at TEXT,
                is_service INTEGER NOT NULL DEFAULT 0
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

        // Protocol related tables
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS protocol_model (
                id INTEGER PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                description TEXT,
                version TEXT,
                created_at TEXT,
                updated_at TEXT,
                enabled INTEGER NOT NULL DEFAULT 1,
                creator INTEGER,
                FOREIGN KEY (creator) REFERENCES user(id) ON DELETE SET NULL
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS protocol_step (
                id INTEGER PRIMARY KEY NOT NULL,
                protocol INTEGER NOT NULL,
                section INTEGER,
                step_number INTEGER NOT NULL,
                title TEXT,
                description TEXT,
                estimated_time INTEGER,
                critical INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (protocol) REFERENCES protocol_model(id) ON DELETE CASCADE,
                FOREIGN KEY (section) REFERENCES protocol_section(id) ON DELETE SET NULL
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS protocol_section (
                id INTEGER PRIMARY KEY NOT NULL,
                protocol INTEGER NOT NULL,
                title TEXT,
                description TEXT,
                section_number INTEGER NOT NULL,
                FOREIGN KEY (protocol) REFERENCES protocol_model(id) ON DELETE CASCADE
            )
        """)

        // Annotation tables
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS annotation (
                id INTEGER PRIMARY KEY NOT NULL,
                content TEXT NOT NULL,
                created_at TEXT,
                updated_at TEXT,
                step INTEGER,
                session INTEGER,
                user INTEGER,
                stored_reagent INTEGER,
                folder INTEGER,
                FOREIGN KEY (step) REFERENCES protocol_step(id) ON DELETE CASCADE,
                FOREIGN KEY (session) REFERENCES session(id) ON DELETE CASCADE,
                FOREIGN KEY (user) REFERENCES user(id) ON DELETE SET NULL,
                FOREIGN KEY (stored_reagent) REFERENCES stored_reagent(id) ON DELETE CASCADE,
                FOREIGN KEY (folder) REFERENCES annotation_folder(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS annotation_folder (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                parent_folder INTEGER,
                session INTEGER,
                instrument INTEGER,
                stored_reagent INTEGER,
                created_at TEXT,
                FOREIGN KEY (parent_folder) REFERENCES annotation_folder(id) ON DELETE CASCADE,
                FOREIGN KEY (session) REFERENCES session(id) ON DELETE CASCADE,
                FOREIGN KEY (instrument) REFERENCES instrument(id) ON DELETE CASCADE,
                FOREIGN KEY (stored_reagent) REFERENCES stored_reagent(id) ON DELETE CASCADE
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
                title TEXT,
                description TEXT,
                created_at TEXT,
                updated_at TEXT,
                user INTEGER,
                FOREIGN KEY (step) REFERENCES protocol_step(id) ON DELETE CASCADE,
                FOREIGN KEY (user) REFERENCES user(id) ON DELETE SET NULL
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

        // Time tracking
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS time_keeper (
                id INTEGER PRIMARY KEY NOT NULL,
                session INTEGER NOT NULL,
                step INTEGER NOT NULL,
                start_time TEXT NOT NULL,
                end_time TEXT,
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
                batch_number TEXT,
                location TEXT,
                expiration_date TEXT,
                opening_date TEXT,
                current_quantity REAL,
                initial_quantity REAL,
                quantity_unit TEXT,
                storage_object_id INTEGER,
                user_id INTEGER,
                notify_on_expiry INTEGER NOT NULL DEFAULT 0,
                notify_on_low_stock INTEGER NOT NULL DEFAULT 0,
                low_stock_threshold REAL,
                created_at TEXT,
                updated_at TEXT,
                FOREIGN KEY (reagent_id) REFERENCES reagent(id) ON DELETE CASCADE,
                FOREIGN KEY (storage_object_id) REFERENCES storage_object(id) ON DELETE SET NULL,
                FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS reagent_subscription (
                id INTEGER PRIMARY KEY NOT NULL,
                user_id INTEGER NOT NULL,
                stored_reagent INTEGER NOT NULL,
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
                last_warranty_notification_sent TEXT,
                last_maintenance_notification_sent TEXT,
                days_before_warranty_notification INTEGER DEFAULT 30,
                days_before_maintenance_notification INTEGER DEFAULT 14
            )
        """)

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS instrument_usage (
                id INTEGER PRIMARY KEY NOT NULL,
                instrument INTEGER NOT NULL,
                user TEXT NOT NULL,
                start_time TEXT NOT NULL,
                end_time TEXT,
                notes TEXT,
                FOREIGN KEY (instrument) REFERENCES instrument(id) ON DELETE CASCADE,
                FOREIGN KEY (user) REFERENCES user(username)
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
                name TEXT NOT NULL,
                description TEXT,
                type TEXT NOT NULL,
                stored_at INTEGER,
                created_at TEXT,
                updated_at TEXT,
                FOREIGN KEY (stored_at) REFERENCES storage_object(id) ON DELETE CASCADE
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
                name TEXT NOT NULL,
                user INTEGER,
                service_lab_group INTEGER,
                lab_group INTEGER,
                field_mask_mapping TEXT,
                created_at TEXT,
                updated_at TEXT,
                FOREIGN KEY (user) REFERENCES user(id) ON DELETE SET NULL,
                FOREIGN KEY (service_lab_group) REFERENCES lab_group(id) ON DELETE CASCADE,
                FOREIGN KEY (lab_group) REFERENCES lab_group(id) ON DELETE CASCADE
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
                `rememberCredentials` INTEGER NOT NULL DEFAULT 0,
                `theme` TEXT NOT NULL DEFAULT 'system',
                `notificationsEnabled` INTEGER NOT NULL DEFAULT 1,
                `syncFrequency` INTEGER NOT NULL DEFAULT 15,
                `syncOnWifiOnly` INTEGER NOT NULL DEFAULT 1,
                `lastSyncTimestamp` INTEGER NOT NULL DEFAULT 0,
                `isActive` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`userId`, `hostname`)
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
    }
}