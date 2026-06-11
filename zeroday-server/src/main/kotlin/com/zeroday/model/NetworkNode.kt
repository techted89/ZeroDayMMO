package com.zeroday.model

import kotlinx.serialization.Serializable

@Serializable
data class NetworkNode(
    val ip: String,
    val hostname: String,
    val nodeType: NodeType,
    val securityLevel: Int,
    val ports: List<Int> = emptyList(),
    val services: List<String> = emptyList(),
    val vulnerabilities: List<String> = emptyList(),
    val connectedNodes: List<String> = emptyList(),
    val dataFiles: List<String> = emptyList(),
    val isDiscovered: Boolean = false,
    val isCompromised: Boolean = false,
    val ownerId: String? = null,
    val accessLevel: AccessLevel = AccessLevel.NONE
)

@Serializable
enum class NodeType(val displayName: String) {
    ROUTER("Router"),
    SERVER("Server"),
    WORKSTATION("Workstation"),
    DATABASE("Database"),
    FIREWALL("Firewall"),
    HONEYPOT("Honeypot"),
    DARKNET("Darknet Node"),
    IOT("IoT Device"),
    MOBILE("Mobile Device"),
    CLOUD("Cloud Instance"),
    SATELLITE("Satellite Uplink")
}

@Serializable
enum class AccessLevel(val displayName: String) {
    NONE("None"),
    GUEST("Guest"),
    USER("User"),
    ADMIN("Administrator"),
    ROOT("Root")
}

object NetworkTopology {
    fun generateInitialNodes(): List<NetworkNode> {
        val nodes = mutableListOf<NetworkNode>()
        nodes.add(NetworkNode(
            "127.0.0.1", "localhost", NodeType.WORKSTATION, 0,
            listOf(22, 80, 443), listOf("ssh", "http", "https"),
            connectedNodes = listOf("10.0.0.1", "10.0.0.2"),
            dataFiles = listOf("notes.txt", "config.json"),
            isDiscovered = true, isCompromised = true, accessLevel = AccessLevel.ROOT
        ))
        nodes.add(NetworkNode(
            "10.0.0.1", "gateway", NodeType.ROUTER, 2,
            listOf(22, 80, 443, 8080), listOf("ssh", "http", "https", "proxy"),
            vulnerabilities = listOf("default_credentials"),
            connectedNodes = listOf("127.0.0.1", "10.0.0.2", "10.0.0.5", "10.0.1.1"),
            isDiscovered = true
        ))
        nodes.add(NetworkNode(
            "10.0.0.2", "fileserver", NodeType.SERVER, 3,
            listOf(21, 22, 80, 445), listOf("ftp", "ssh", "http", "samba"),
            vulnerabilities = listOf("open_ftp", "weak_smb"),
            connectedNodes = listOf("10.0.0.1", "10.0.0.3"),
            dataFiles = listOf("shared.doc", "backup.zip")
        ))
        nodes.add(NetworkNode(
            "10.0.0.3", "mailserver", NodeType.SERVER, 4,
            listOf(25, 110, 143, 587, 993), listOf("smtp", "pop3", "imap", "submission", "imaps"),
            vulnerabilities = listOf("open_relay", "weak_smtp"),
            connectedNodes = listOf("10.0.0.2", "10.0.1.5")
        ))
        nodes.add(NetworkNode(
            "10.0.0.5", "printer", NodeType.IOT, 1,
            listOf(80, 515, 631), listOf("http", "printer", "ipp"),
            vulnerabilities = listOf("default_password", "unsecured"),
            connectedNodes = listOf("10.0.0.1")
        ))
        nodes.add(NetworkNode(
            "10.0.1.1", "corp-router", NodeType.ROUTER, 5,
            listOf(22, 443, 8443), listOf("ssh", "https", "alt-https"),
            vulnerabilities = listOf("old_firmware"),
            connectedNodes = listOf("10.0.0.1", "10.0.1.5", "10.0.1.10", "10.0.1.200")
        ))
        nodes.add(NetworkNode(
            "10.0.1.5", "vpn-server", NodeType.SERVER, 6,
            listOf(22, 443, 1194, 1723), listOf("ssh", "https", "openvpn", "pptp"),
            vulnerabilities = listOf("pptp_vuln", "heartbleed"),
            connectedNodes = listOf("10.0.1.1")
        ))
        nodes.add(NetworkNode(
            "10.0.1.10", "dev-db", NodeType.DATABASE, 6,
            listOf(22, 3306, 5432), listOf("ssh", "mysql", "postgresql"),
            vulnerabilities = listOf("weak_mysql_root"),
            connectedNodes = listOf("10.0.1.1"),
            dataFiles = listOf("users.sql", "schema.sql")
        ))
        nodes.add(NetworkNode(
            "10.0.1.200", "webapp", NodeType.SERVER, 7,
            listOf(22, 80, 443, 8080, 8443), listOf("ssh", "http", "https", "proxy", "alt-https"),
            vulnerabilities = listOf("sqli", "xss", "lfi"),
            connectedNodes = listOf("10.0.1.1", "10.0.2.10")
        ))
        nodes.add(NetworkNode(
            "10.0.2.10", "internal-fs", NodeType.SERVER, 8,
            listOf(22, 445, 3389), listOf("ssh", "samba", "rdp"),
            vulnerabilities = listOf("eternalblue"),
            connectedNodes = listOf("10.0.1.200", "10.0.2.50", "172.16.0.1"),
            dataFiles = listOf("credentials.xlsx", "network_map.pdf")
        ))
        nodes.add(NetworkNode(
            "10.0.2.50", "hr-database", NodeType.DATABASE, 9,
            listOf(22, 1433, 1521), listOf("ssh", "mssql", "oracle"),
            vulnerabilities = listOf("weak_sa_password"),
            connectedNodes = listOf("10.0.2.10")
        ))
        nodes.add(NetworkNode(
            "172.16.0.1", "darknet-gateway", NodeType.DARKNET, 10,
            listOf(22, 80, 443, 6667, 9001), listOf("ssh", "http", "https", "irc", "tor"),
            vulnerabilities = listOf(),
            connectedNodes = listOf("10.0.2.10", "172.16.0.50", "203.0.113.1")
        ))
        nodes.add(NetworkNode(
            "172.16.0.50", "marketplace", NodeType.DARKNET, 12,
            listOf(80, 443), listOf("http", "https"),
            vulnerabilities = listOf("sqli"),
            connectedNodes = listOf("172.16.0.1", "172.16.0.100"),
            dataFiles = listOf("listings.db")
        ))
        nodes.add(NetworkNode(
            "172.16.0.100", "forum", NodeType.DARKNET, 13,
            listOf(80, 443), listOf("http", "https"),
            vulnerabilities = listOf("xss", "csrf"),
            connectedNodes = listOf("172.16.0.50"),
            dataFiles = listOf("users.db")
        ))
        nodes.add(NetworkNode(
            "203.0.113.1", "backbone-router", NodeType.ROUTER, 14,
            listOf(22, 443), listOf("ssh", "https"),
            vulnerabilities = listOf("backdoor"),
            connectedNodes = listOf("172.16.0.1", "198.51.100.1", "203.0.113.50")
        ))
        nodes.add(NetworkNode(
            "203.0.113.50", "gov-server", NodeType.SERVER, 17,
            listOf(22, 443, 8443), listOf("ssh", "https", "alt-https"),
            vulnerabilities = listOf("zero_day"),
            connectedNodes = listOf("203.0.113.1"),
            dataFiles = listOf("classified.bin", "communications.enc")
        ))
        nodes.add(NetworkNode(
            "198.51.100.1", "isp-router", NodeType.ROUTER, 11,
            listOf(22, 443), listOf("ssh", "https"),
            vulnerabilities = listOf("default_community_string"),
            connectedNodes = listOf("203.0.113.1", "10.0.3.1", "10.0.4.1")
        ))
        nodes.add(NetworkNode(
            "10.0.3.1", "cloud-gateway", NodeType.CLOUD, 9,
            listOf(22, 443, 8443), listOf("ssh", "https", "alt-https"),
            vulnerabilities = listOf("misconfigured_s3"),
            connectedNodes = listOf("198.51.100.1", "10.0.3.100", "10.0.3.200")
        ))
        nodes.add(NetworkNode(
            "10.0.3.100", "cloud-server", NodeType.CLOUD, 10,
            listOf(22, 80, 443, 3306), listOf("ssh", "http", "https", "mysql"),
            vulnerabilities = listOf("weak_ssh_key"),
            connectedNodes = listOf("10.0.3.1"),
            dataFiles = listOf("config.php", ".env")
        ))
        nodes.add(NetworkNode(
            "10.0.3.200", "ecommerce", NodeType.SERVER, 10,
            listOf(22, 80, 443), listOf("ssh", "http", "https"),
            vulnerabilities = listOf("sqli", "xss", "lfi"),
            connectedNodes = listOf("10.0.3.1"),
            dataFiles = listOf("products.db", "orders.db")
        ))
        nodes.add(NetworkNode(
            "10.0.4.1", "mil-router", NodeType.ROUTER, 15,
            listOf(22, 443, 8443), listOf("ssh", "https", "alt-https"),
            vulnerabilities = listOf("backdoor"),
            connectedNodes = listOf("198.51.100.1", "10.0.4.20", "10.0.5.1")
        ))
        nodes.add(NetworkNode(
            "10.0.4.20", "mil-server", NodeType.SERVER, 16,
            listOf(22, 443, 3389), listOf("ssh", "https", "rdp"),
            vulnerabilities = listOf("eternalblue", "doublepulsar"),
            connectedNodes = listOf("10.0.4.1"),
            dataFiles = listOf("drone_codes.bin", "patrol_schedules.pdf")
        ))
        nodes.add(NetworkNode(
            "10.0.5.1", "sat-uplink", NodeType.SATELLITE, 18,
            listOf(22, 443, 2100), listOf("ssh", "https", "satcom"),
            vulnerabilities = listOf("weak_encryption"),
            connectedNodes = listOf("10.0.4.1", "10.0.5.100")
        ))
        nodes.add(NetworkNode(
            "10.0.5.100", "corp-hq", NodeType.SERVER, 19,
            listOf(22, 443, 8080), listOf("ssh", "https", "proxy"),
            vulnerabilities = listOf("side_channel"),
            connectedNodes = listOf("10.0.5.1"),
            dataFiles = listOf("ceo_emails.enc", "financials.xlsx")
        ))
        return nodes
    }
}
