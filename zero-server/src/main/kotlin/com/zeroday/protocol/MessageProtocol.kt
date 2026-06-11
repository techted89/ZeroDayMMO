package com.zeroday.protocol

/**
 * Wire format for WebSocket messages exchanged between the server and the Unity client.
 *
 * Inbound: clients send `{"type":"<command>","payload":{...}}`.
 * Outbound: the server replies with the same envelope, `type` is the response name.
 */
object MessageProtocol {
    const val FIELD_TYPE = "type"
    const val FIELD_PAYLOAD = "payload"
    const val FIELD_TIMESTAMP = "timestamp"
    const val FIELD_ERROR = "error"
}

/**
 * Standard server response type names. Centralizing these avoids "magic strings"
 * scattered across handlers and keeps the protocol self-documenting.
 */
object ResponseTypes {
    const val ERROR = "error"
    const val PONG = "pong"

    const val REGISTER_SUCCESS = "register_success"
    const val LOGIN_SUCCESS = "login_success"
    const val LOGOUT_SUCCESS = "logout_success"

    const val STATUS = "status"
    const val ONLINE_PLAYERS = "online_players"
    const val LEADERBOARD = "leaderboard"

    const val COMMAND_RESULT = "command_result"

    const val STORYLINES = "storylines"
    const val STORYLINE_STARTED = "storyline_started"
    const val STORY_ADVANCED = "story_advanced"

    const val TASKS = "tasks"
    const val TASK_ACCEPTED = "task_accepted"
    const val TASK_COMPLETED = "task_completed"

    const val NETWORK = "network"
    const val NODE_DISCOVERED = "node_discovered"
    const val SUBNET_SCANNED = "subnet_scanned"

    const val PARTY_CREATED = "party_created"
    const val PARTY_JOINED = "party_joined"

    const val FACTION_CREATED = "faction_created"
    const val FACTION_JOINED = "faction_joined"
    const val FACTION_LEFT = "faction_left"
    const val FACTION_INFO = "faction_info"
    const val FACTION_LIST = "faction_list"
    const val FACTION_DONATED = "faction_donated"
    const val FACTION_UPGRADED = "faction_upgraded"

    const val RESEARCH_RECIPES = "research_recipes"
    const val RESEARCH_INVENTORY = "research_inventory"
    const val FRAGMENT_GATHERED = "fragment_gathered"
    const val RESEARCH_STARTED = "research_started"
    const val RESEARCH_CLAIMED = "research_claimed"
    const val ITEM_USED = "item_used"
    const val RESEARCH_STATUS = "research_status"

    const val NEXUS_RESULT = "nexus_result"
    const val NEXUS_ERROR = "nexus_error"
    const val SCRIPT_SAVED = "script_saved"
    const val SCRIPT_LIST = "script_list"
    const val SCRIPT_DELETED = "script_deleted"
    const val VALIDATION_RESULT = "validation_result"

    const val KNOWLEDGE_MAP = "knowledge_map"
    const val FRAGMENT_DISCOVERED = "fragment_discovered"
    const val AVAILABLE_FRAGMENTS = "available_fragments"

    const val WORLD_EVENTS = "world_events"
    const val WORLD_EVENT_JOINED = "world_event_joined"
    const val WORLD_EVENT_LEFT = "world_event_left"

    const val AD_REWARD = "ad_reward"
    const val AD_COOLDOWN = "ad_cooldown"

    const val ACHIEVEMENTS = "achievements"
    const val ACHIEVEMENT_UNLOCKED = "achievement_unlocked"
    const val ACHIEVEMENT_PROGRESS = "achievement_progress"

    const val CHALLENGES = "challenges"
    const val CHALLENGE_COMPLETED = "challenge_completed"
    const val CHALLENGE_ROTATED = "challenge_rotated"

    const val NOTIFICATIONS = "notifications"
    const val NOTIFICATION_ADDED = "notification_added"

    const val SKILL_TREE = "skill_tree"
    const val SKILL_UNLOCKED = "skill_unlocked"

    const val PROFILE = "profile"

    const val CAREER_CHOSEN = "career_chosen"
    const val CAREER_STATUS = "career_status"
    const val HEAT_UPDATED = "heat_updated"
    const val ARREST_SUCCESS = "arrest_success"
    const val ZONE_INFO = "zone_info"
    const val ZONE_TRAVEL_STARTED = "zone_travel_started"
    const val ZONE_TRAVEL_COMPLETE = "zone_travel_complete"
    const val ZONE_LIST = "zone_list"
    const val ZONE_ATTACK_RESULT = "zone_attack_result"
    const val ZONE_CLAIMED = "zone_claimed"
    const val FACTION_CYCLE = "faction_cycle"
    const val HUNTER_STATUS = "hunter_status"

    // Auction house
    const val AUCTION_LISTINGS = "auction_listings"
    const val AUCTION_SEARCH_RESULTS = "auction_search_results"
    const val AUCTION_CREATED = "auction_created"
    const val AUCTION_BID_PLACED = "auction_bid_placed"
    const val AUCTION_BOUGHT = "auction_bought"
    const val AUCTION_MY_LISTINGS = "auction_my_listings"
    const val AUCTION_CANCELLED = "auction_cancelled"

    // Co-op boss fights
    const val BOSS_INSTANCE_CREATED = "boss_instance_created"
    const val BOSS_INSTANCE_JOINED = "boss_instance_joined"
    const val BOSS_ACTION_RESULT = "boss_action_result"
    const val BOSS_STATUS = "boss_status"
    const val BOSS_AVAILABLE_INSTANCES = "boss_available_instances"

    // Game events (PvP)
    const val EVENT_LIST = "event_list"
    const val EVENT_JOINED = "event_joined"
    const val EVENT_LEFT = "event_left"
    const val EVENT_LEADERBOARD = "event_leaderboard"
    const val EVENT_SCORE_UPDATED = "event_score_updated"
    const val EVENT_MY_EVENTS = "event_my_events"

    // Server push events (sent to subscribed channels)
    const val PUSH_NOTIFICATION = "push_notification"
    const val PUSH_ACHIEVEMENT = "push_achievement"
    const val PUSH_CHALLENGE = "push_challenge"
    const val PUSH_WORLD_EVENT = "push_world_event"
    const val PUSH_RESOURCE_UPDATE = "push_resource_update"
    const val PUSH_SERVER_ANNOUNCEMENT = "push_server_announcement"

    // Channel subscription management
    const val SUBSCRIBED = "subscribed"
    const val UNSUBSCRIBED = "unsubscribed"
    const val SUBSCRIPTIONS = "subscriptions"  // current subscription list
}

/**
 * Standard client request type names. Mirrors the keys in WebSocketHandler's
 * `processMessage` `when` so they can be looked up by name.
 */
object RequestTypes {
    const val REGISTER = "register"
    const val LOGIN = "login"
    const val LOGOUT = "logout"
    const val PING = "ping"

    const val COMMAND = "command"
    const val GET_STATUS = "get_status"
    const val GET_ONLINE_PLAYERS = "get_online_players"
    const val GET_LEADERBOARD = "get_leaderboard"

    const val GET_STORYLINES = "get_storylines"
    const val START_STORYLINE = "start_storyline"
    const val ADVANCE_STORY = "advance_story"

    const val GET_TASKS = "get_tasks"
    const val ACCEPT_TASK = "accept_task"
    const val COMPLETE_TASK = "complete_task"

    const val GET_NETWORK = "get_network"
    const val DISCOVER_NODE = "discover_node"
    const val SCAN_SUBNET = "scan_subnet"

    const val CREATE_PARTY = "create_party"
    const val JOIN_PARTY = "join_party"

    const val FACTION_CREATE = "faction_create"
    const val FACTION_JOIN = "faction_join"
    const val FACTION_LEAVE = "faction_leave"
    const val FACTION_INFO = "faction_info"
    const val FACTION_LIST = "faction_list"
    const val FACTION_DONATE = "faction_donate"
    const val FACTION_UPGRADE = "faction_upgrade"

    const val RESEARCH_RECIPES = "research_recipes"
    const val RESEARCH_INVENTORY = "research_inventory"
    const val RESEARCH_GATHER = "research_gather"
    const val RESEARCH_START = "research_start"
    const val RESEARCH_CLAIM = "research_claim"
    const val RESEARCH_USE_ITEM = "research_use_item"
    const val RESEARCH_STATUS = "research_status"

    const val NEXUS_RUN = "nexus_run"
    const val NEXUS_SAVE = "nexus_save"
    const val NEXUS_LIST = "nexus_list"
    const val NEXUS_DELETE = "nexus_delete"
    const val NEXUS_VALIDATE = "nexus_validate"

    const val KMAP_GET = "kmap_get"
    const val KMAP_DISCOVER = "kmap_discover"
    const val KMAP_FRAGMENTS = "kmap_fragments"

    const val WORLD_EVENTS = "world_events"
    const val WORLD_EVENT_JOIN = "world_event_join"
    const val WORLD_EVENT_LEAVE = "world_event_leave"

    const val WATCH_AD = "watch_ad"

    const val GET_ACHIEVEMENTS = "get_achievements"
    const val CLAIM_ACHIEVEMENT = "claim_achievement"

    const val GET_CHALLENGES = "get_challenges"
    const val CLAIM_CHALLENGE = "claim_challenge"
    const val ROTATE_CHALLENGES = "rotate_challenges"

    const val GET_NOTIFICATIONS = "get_notifications"
    const val MARK_NOTIFICATION_READ = "mark_notification_read"
    const val MARK_ALL_NOTIFICATIONS_READ = "mark_all_notifications_read"

    const val GET_SKILL_TREE = "get_skill_tree"
    const val UNLOCK_SKILL = "unlock_skill"

    const val GET_PROFILE = "get_profile"

    const val CAREER_CHOOSE = "career_choose"
    const val CAREER_STATUS = "career_status"
    const val CAREER_ADD_HEAT = "career_add_heat"
    const val CAREER_ARREST = "career_arrest"

    const val ZONE_INFO = "zone_info"
    const val ZONE_TRAVEL = "zone_travel"
    const val ZONE_LIST = "zone_list"
    const val ZONE_ATTACK = "zone_attack"
    const val ZONE_CLAIM = "zone_claim"
    const val FACTION_CYCLE_INFO = "faction_cycle_info"
    const val GET_HUNTER_STATUS = "get_hunter_status"

    // Auction house
    const val AUCTION_LIST = "auction_list"
    const val AUCTION_SEARCH = "auction_search"
    const val AUCTION_CREATE = "auction_create"
    const val AUCTION_BID = "auction_bid"
    const val AUCTION_BUYOUT = "auction_buyout"
    const val AUCTION_MY_LISTINGS = "auction_my_listings"
    const val AUCTION_CANCEL = "auction_cancel"

    // Co-op boss fights
    const val BOSS_CREATE = "boss_create"
    const val BOSS_JOIN = "boss_join"
    const val BOSS_ACTION = "boss_action"
    const val BOSS_STATUS = "boss_status"
    const val BOSS_LIST_AVAILABLE = "boss_list_available"

    // Game events (PvP)
    const val EVENT_LIST = "event_list"
    const val EVENT_JOIN = "event_join"
    const val EVENT_LEAVE = "event_leave"
    const val EVENT_LEADERBOARD = "event_leaderboard"
    const val EVENT_SCORE = "event_score"
    const val EVENT_MY_EVENTS = "event_my_events"

    // Channel subscription management
    const val SUBSCRIBE = "subscribe"
    const val UNSUBSCRIBE = "unsubscribe"
}

/**
 * Lightweight error categories so the client can present them sensibly.
 */
enum class ErrorCategory {
    VALIDATION,
    AUTHENTICATION,
    AUTHORIZATION,
    NOT_FOUND,
    CONFLICT,
    RATE_LIMITED,
    INTERNAL
}

data class ServerError(
    val code: String,
    val message: String,
    val category: ErrorCategory = ErrorCategory.INTERNAL
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "code" to code,
        "message" to message,
        "category" to category.name
    )

    companion object {
        fun fromMessage(message: String, category: ErrorCategory = ErrorCategory.INTERNAL): ServerError =
            ServerError(code = category.name.lowercase(), message = message, category = category)
    }
}
