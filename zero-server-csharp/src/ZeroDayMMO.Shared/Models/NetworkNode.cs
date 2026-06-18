using System.Text.Json;
using System.Text.Json.Serialization;

namespace ZeroDayMMO.Shared.Models;

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum NodeType
{
    [JsonPropertyName("router")]
    ROUTER,
    [JsonPropertyName("server")]
    SERVER,
    [JsonPropertyName("workstation")]
    WORKSTATION,
    [JsonPropertyName("database")]
    DATABASE,
    [JsonPropertyName("firewall")]
    FIREWALL,
    [JsonPropertyName("honeypot")]
    HONEYPOT,
    [JsonPropertyName("darknet")]
    DARKNET,
    [JsonPropertyName("iot")]
    IOT,
    [JsonPropertyName("mobile")]
    MOBILE,
    [JsonPropertyName("cloud")]
    CLOUD,
    [JsonPropertyName("satellite")]
    SATELLITE
}

public static class NodeTypeExtensions
{
    public static string DisplayName(this NodeType type) => type switch
    {
        NodeType.ROUTER => "Router",
        NodeType.SERVER => "Server",
        NodeType.WORKSTATION => "Workstation",
        NodeType.DATABASE => "Database",
        NodeType.FIREWALL => "Firewall",
        NodeType.HONEYPOT => "Honeypot",
        NodeType.DARKNET => "Darknet Node",
        NodeType.IOT => "IoT Device",
        NodeType.MOBILE => "Mobile Device",
        NodeType.CLOUD => "Cloud Instance",
        NodeType.SATELLITE => "Satellite Uplink",
        _ => "Unknown"
    };
}

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum AccessLevel
{
    [JsonPropertyName("none")]
    NONE,
    [JsonPropertyName("guest")]
    GUEST,
    [JsonPropertyName("user")]
    USER,
    [JsonPropertyName("admin")]
    ADMIN,
    [JsonPropertyName("root")]
    ROOT
}

public static class AccessLevelExtensions
{
    public static string DisplayName(this AccessLevel level) => level switch
    {
        AccessLevel.NONE => "None",
        AccessLevel.GUEST => "Guest",
        AccessLevel.USER => "User",
        AccessLevel.ADMIN => "Administrator",
        AccessLevel.ROOT => "Root",
        _ => "None"
    };
}

public class NetworkNode
{
    [JsonPropertyName("ip")]
    public string Ip { get; set; } = "";

    [JsonPropertyName("hostname")]
    public string Hostname { get; set; } = "";

    [JsonPropertyName("node_type")]
    public NodeType NodeType { get; set; }

    [JsonPropertyName("security_level")]
    public int SecurityLevel { get; set; }

    [JsonPropertyName("ports")]
    public List<int> Ports { get; set; } = new();

    [JsonPropertyName("services")]
    public List<string> Services { get; set; } = new();

    [JsonPropertyName("vulnerabilities")]
    public List<string> Vulnerabilities { get; set; } = new();

    [JsonPropertyName("connected_nodes")]
    public List<string> ConnectedNodes { get; set; } = new();

    [JsonPropertyName("data_files")]
    public List<string> DataFiles { get; set; } = new();

    [JsonPropertyName("is_discovered")]
    public bool IsDiscovered { get; set; } = false;

    [JsonPropertyName("is_compromised")]
    public bool IsCompromised { get; set; } = false;

    [JsonPropertyName("owner_id")]
    public string? OwnerId { get; set; }

    [JsonPropertyName("access_level")]
    public AccessLevel AccessLevel { get; set; } = AccessLevel.NONE;
}

public static class NetworkTopology
{
    public static List<NetworkNode> GenerateInitialNodes() => new()
    {
        new NetworkNode { Ip = "127.0.0.1", Hostname = "localhost", NodeType = NodeType.WORKSTATION, SecurityLevel = 0, Ports = new() { 22, 80, 443 }, Services = new() { "ssh", "http", "https" }, ConnectedNodes = new() { "10.0.0.1", "10.0.0.2" }, DataFiles = new() { "notes.txt", "config.json" }, IsDiscovered = true, IsCompromised = true, AccessLevel = AccessLevel.ROOT },
        new NetworkNode { Ip = "10.0.0.1", Hostname = "gateway", NodeType = NodeType.ROUTER, SecurityLevel = 2, Ports = new() { 22, 80, 443, 8080 }, Services = new() { "ssh", "http", "https", "proxy" }, Vulnerabilities = new() { "default_credentials" }, ConnectedNodes = new() { "127.0.0.1", "10.0.0.2", "10.0.0.5", "10.0.1.1" }, IsDiscovered = true },
        new NetworkNode { Ip = "10.0.0.2", Hostname = "fileserver", NodeType = NodeType.SERVER, SecurityLevel = 3, Ports = new() { 21, 22, 80, 445 }, Services = new() { "ftp", "ssh", "http", "samba" }, Vulnerabilities = new() { "open_ftp", "weak_smb" }, ConnectedNodes = new() { "10.0.0.1", "10.0.0.3" }, DataFiles = new() { "shared.doc", "backup.zip" } },
        new NetworkNode { Ip = "10.0.0.3", Hostname = "mailserver", NodeType = NodeType.SERVER, SecurityLevel = 4, Ports = new() { 25, 110, 143, 587, 993 }, Services = new() { "smtp", "pop3", "imap", "submission", "imaps" }, Vulnerabilities = new() { "open_relay", "weak_smtp" }, ConnectedNodes = new() { "10.0.0.2", "10.0.1.5" } },
        new NetworkNode { Ip = "10.0.0.5", Hostname = "printer", NodeType = NodeType.IOT, SecurityLevel = 1, Ports = new() { 80, 515, 631 }, Services = new() { "http", "printer", "ipp" }, Vulnerabilities = new() { "default_password", "unsecured" }, ConnectedNodes = new() { "10.0.0.1" } },
        new NetworkNode { Ip = "10.0.1.1", Hostname = "corp-router", NodeType = NodeType.ROUTER, SecurityLevel = 5, Ports = new() { 22, 443, 8443 }, Services = new() { "ssh", "https", "alt-https" }, Vulnerabilities = new() { "old_firmware" }, ConnectedNodes = new() { "10.0.0.1", "10.0.1.5", "10.0.1.10", "10.0.1.200" } },
        new NetworkNode { Ip = "10.0.1.5", Hostname = "vpn-server", NodeType = NodeType.SERVER, SecurityLevel = 6, Ports = new() { 22, 443, 1194, 1723 }, Services = new() { "ssh", "https", "openvpn", "pptp" }, Vulnerabilities = new() { "pptp_vuln", "heartbleed" }, ConnectedNodes = new() { "10.0.1.1" } },
        new NetworkNode { Ip = "10.0.1.10", Hostname = "dev-db", NodeType = NodeType.DATABASE, SecurityLevel = 6, Ports = new() { 22, 3306, 5432 }, Services = new() { "ssh", "mysql", "postgresql" }, Vulnerabilities = new() { "weak_mysql_root" }, ConnectedNodes = new() { "10.0.1.1" }, DataFiles = new() { "users.sql", "schema.sql" } },
        new NetworkNode { Ip = "10.0.1.200", Hostname = "webapp", NodeType = NodeType.SERVER, SecurityLevel = 7, Ports = new() { 22, 80, 443, 8080, 8443 }, Services = new() { "ssh", "http", "https", "proxy", "alt-https" }, Vulnerabilities = new() { "sqli", "xss", "lfi" }, ConnectedNodes = new() { "10.0.1.1", "10.0.2.10" } },
        new NetworkNode { Ip = "10.0.2.10", Hostname = "internal-fs", NodeType = NodeType.SERVER, SecurityLevel = 8, Ports = new() { 22, 445, 3389 }, Services = new() { "ssh", "samba", "rdp" }, Vulnerabilities = new() { "eternalblue" }, ConnectedNodes = new() { "10.0.1.200", "10.0.2.50", "172.16.0.1" }, DataFiles = new() { "credentials.xlsx", "network_map.pdf" } },
        new NetworkNode { Ip = "10.0.2.50", Hostname = "hr-database", NodeType = NodeType.DATABASE, SecurityLevel = 9, Ports = new() { 22, 1433, 1521 }, Services = new() { "ssh", "mssql", "oracle" }, Vulnerabilities = new() { "weak_sa_password" }, ConnectedNodes = new() { "10.0.2.10" } },
        new NetworkNode { Ip = "172.16.0.1", Hostname = "darknet-gateway", NodeType = NodeType.DARKNET, SecurityLevel = 10, Ports = new() { 22, 80, 443, 6667, 9001 }, Services = new() { "ssh", "http", "https", "irc", "tor" }, ConnectedNodes = new() { "10.0.2.10", "172.16.0.50", "203.0.113.1" } },
        new NetworkNode { Ip = "172.16.0.50", Hostname = "marketplace", NodeType = NodeType.DARKNET, SecurityLevel = 12, Ports = new() { 80, 443 }, Services = new() { "http", "https" }, Vulnerabilities = new() { "sqli" }, ConnectedNodes = new() { "172.16.0.1", "172.16.0.100" }, DataFiles = new() { "listings.db" } },
        new NetworkNode { Ip = "172.16.0.100", Hostname = "forum", NodeType = NodeType.DARKNET, SecurityLevel = 13, Ports = new() { 80, 443 }, Services = new() { "http", "https" }, Vulnerabilities = new() { "xss", "csrf" }, ConnectedNodes = new() { "172.16.0.50" }, DataFiles = new() { "users.db" } },
        new NetworkNode { Ip = "203.0.113.1", Hostname = "backbone-router", NodeType = NodeType.ROUTER, SecurityLevel = 14, Ports = new() { 22, 443 }, Services = new() { "ssh", "https" }, Vulnerabilities = new() { "backdoor" }, ConnectedNodes = new() { "172.16.0.1", "198.51.100.1", "203.0.113.50" } },
        new NetworkNode { Ip = "203.0.113.50", Hostname = "gov-server", NodeType = NodeType.SERVER, SecurityLevel = 17, Ports = new() { 22, 443, 8443 }, Services = new() { "ssh", "https", "alt-https" }, Vulnerabilities = new() { "zero_day" }, ConnectedNodes = new() { "203.0.113.1" }, DataFiles = new() { "classified.bin", "communications.enc" } },
        new NetworkNode { Ip = "198.51.100.1", Hostname = "isp-router", NodeType = NodeType.ROUTER, SecurityLevel = 11, Ports = new() { 22, 443 }, Services = new() { "ssh", "https" }, Vulnerabilities = new() { "default_community_string" }, ConnectedNodes = new() { "203.0.113.1", "10.0.3.1", "10.0.4.1" } },
        new NetworkNode { Ip = "10.0.3.1", Hostname = "cloud-gateway", NodeType = NodeType.CLOUD, SecurityLevel = 9, Ports = new() { 22, 443, 8443 }, Services = new() { "ssh", "https", "alt-https" }, Vulnerabilities = new() { "misconfigured_s3" }, ConnectedNodes = new() { "198.51.100.1", "10.0.3.100", "10.0.3.200" } },
        new NetworkNode { Ip = "10.0.3.100", Hostname = "cloud-server", NodeType = NodeType.CLOUD, SecurityLevel = 10, Ports = new() { 22, 80, 443, 3306 }, Services = new() { "ssh", "http", "https", "mysql" }, Vulnerabilities = new() { "weak_ssh_key" }, ConnectedNodes = new() { "10.0.3.1" }, DataFiles = new() { "config.php", ".env" } },
        new NetworkNode { Ip = "10.0.3.200", Hostname = "ecommerce", NodeType = NodeType.SERVER, SecurityLevel = 10, Ports = new() { 22, 80, 443 }, Services = new() { "ssh", "http", "https" }, Vulnerabilities = new() { "sqli", "xss", "lfi" }, ConnectedNodes = new() { "10.0.3.1" }, DataFiles = new() { "products.db", "orders.db" } },
        new NetworkNode { Ip = "10.0.4.1", Hostname = "mil-router", NodeType = NodeType.ROUTER, SecurityLevel = 15, Ports = new() { 22, 443, 8443 }, Services = new() { "ssh", "https", "alt-https" }, Vulnerabilities = new() { "backdoor" }, ConnectedNodes = new() { "198.51.100.1", "10.0.4.20", "10.0.5.1" } },
        new NetworkNode { Ip = "10.0.4.20", Hostname = "mil-server", NodeType = NodeType.SERVER, SecurityLevel = 16, Ports = new() { 22, 443, 3389 }, Services = new() { "ssh", "https", "rdp" }, Vulnerabilities = new() { "eternalblue", "doublepulsar" }, ConnectedNodes = new() { "10.0.4.1" }, DataFiles = new() { "drone_codes.bin", "patrol_schedules.pdf" } },
        new NetworkNode { Ip = "10.0.5.1", Hostname = "sat-uplink", NodeType = NodeType.SATELLITE, SecurityLevel = 18, Ports = new() { 22, 443, 2100 }, Services = new() { "ssh", "https", "satcom" }, Vulnerabilities = new() { "weak_encryption" }, ConnectedNodes = new() { "10.0.4.1", "10.0.5.100" } },
        new NetworkNode { Ip = "10.0.5.100", Hostname = "corp-hq", NodeType = NodeType.SERVER, SecurityLevel = 19, Ports = new() { 22, 443, 8080 }, Services = new() { "ssh", "https", "proxy" }, Vulnerabilities = new() { "side_channel" }, ConnectedNodes = new() { "10.0.5.1" }, DataFiles = new() { "ceo_emails.enc", "financials.xlsx" } }
    };
}
