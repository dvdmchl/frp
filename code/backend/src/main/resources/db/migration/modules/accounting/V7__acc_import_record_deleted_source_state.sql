-- records no longer returned by the source count as deleted in the source, so a conflict about the deletion of their
-- transaction can be told apart from a conflict about a change
UPDATE ${schema}.acc_import_record SET source_state = 'DELETED' WHERE status = 'DELETED';
