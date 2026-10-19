CREATE TABLE ce_pack_preferences (
    player_id VARCHAR(36) NOT NULL,
    preferences TEXT NOT NULL DEFAULT ('{}'),
    PRIMARY KEY (player_id)
);
