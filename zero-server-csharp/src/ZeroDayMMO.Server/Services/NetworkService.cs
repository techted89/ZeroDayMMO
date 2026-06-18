using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Services;

public enum NodeType
{
    Router,
    Server,
    Workstation,
    Database,
    Firewall,
    Honeypot,
    Darknet,
    Iot,
    Mobile,
    Cloud,
    Satellite
}

public enum AccessLevel
{
    None,
    Guest,
    User,
    Admin,
    Root
}

public record NetworkNode(
    string Ip,
    string Hostname,
    NodeType NodeType,
    int SecurityLevel,
    List<int> Ports,
    List<string> Services,
    List<string> Vulnerabilities,
    List<string> ConnectedNodes,
    List<string> DataFiles,
    bool IsDiscovered,
    bool IsCompromised,
    string? OwnerId,
    AccessLevel AccessLevel
);

public record NetworkStats(
    int TotalNodes,
    int CompromisedNodes,
    int TotalDiscoveries,
    int OnlinePlayers
);

public static class NetworkTopology
{
    public static List<NetworkNode> GenerateInitialNodes()
    {
        var nodes = new List<NetworkNode>();

        nodes.Add(new("127.0.0.1", "localhost", NodeType.Workstation, 0,
            new() { 22, 80, 443 }, new() { "ssh", "http", "https" },
            new(), new() { "10.0.0.1", "10.0.0.2" }, new() { "notes.txt", "config.json" },
            true, true, null, AccessLevel.Root));

        nodes.Add(new("10.0.0.1", "gateway", NodeType.Router, 2,
            new() { 22, 80, 443, 8080 }, new() { "ssh", "http", "https", "proxy" },
            new() { "default_credentials" }, new() { "127.0.0.1", "10.0.0.2", "10.0.0.5", "10.0.1.1" },
            new(), true, false, null, AccessLevel.None));

        nodes.Add(new("10.0.0.2", "fileserver", NodeType.Server, 3,
            new() { 21, 22, 80, 445 }, new() { "ftp", "ssh", "http", "samba" },
            new() { "open_ftp", "weak_smb" }, new() { "10.0.0.1", "10.0.0.3" },
            new() { "shared.doc", "backup.zip" }, false, false, null, AccessLevel.None));

        nodes.Add(new("10.0.0.3", "mailserver", NodeType.Server, 4,
            new() { 25, 110, 143, 587, 993 }, new() { "smtp", "pop3", "imap", "submission", "imaps" },
            new() { "open_relay", "weak_smtp" }, new() { "10.0.0.2", "10.0.1.5" },
            new(), false, false, null, AccessLevel.None));

        nodes.Add(new("10.0.0.5", "printer", NodeType.Iot, 1,
            new() { 80, 515, 631 }, new() { "http", "printer", "ipp" },
            new() { "default_password", "unsecured" }, new() { "10.0.0.1" },
            new(), false, false, null, AccessLevel.None));

        nodes.Add(new("10.0.0.10", "dns-server", NodeType.Server, 2,
            new() { 22, 53, 953 }, new() { "ssh", "dns", "rndc" },
            new() { "zone_transfer" }, new() { "10.0.0.1", "10.0.1.1" },
            new(), false, false, null, AccessLevel.None));

        nodes.Add(new("10.0.1.1", "corp-router", NodeType.Router, 5,
            new() { 22, 443, 8443 }, new() { "ssh", "https", "alt-https" },
            new() { "old_firmware" }, new() { "10.0.0.1", "10.0.1.5", "10.0.1.10", "10.0.1.200" },
            new(), false, false, null, AccessLevel.None));

        nodes.Add(new("10.0.1.5", "vpn-server", NodeType.Server, 6,
            new() { 22, 443, 1194, 1723 }, new() { "ssh", "https", "openvpn", "pptp" },
            new() { "pptp_vuln", "heartbleed" }, new() { "10.0.1.1" },
            new(), false, false, null, AccessLevel.None));

        nodes.Add(new("10.0.1.10", "dev-db", NodeType.Database, 6,
            new() { 22, 3306, 5432 }, new() { "ssh", "mysql", "postgresql" },
            new() { "weak_mysql_root" }, new() { "10.0.1.1" },
            new() { "users.sql", "schema.sql" }, false, false, null, AccessLevel.None));

        nodes.Add(new("10.0.1.200", "webapp", NodeType.Server, 7,
            new() { 22, 80, 443, 8080, 8443 }, new() { "ssh", "http", "https", "proxy", "alt-https" },
            new() { "sqli", "xss", "lfi" }, new() { "10.0.1.1", "10.0.2.10" },
            new(), false, false, null, AccessLevel.None));

        nodes.Add(new("10.0.2.10", "internal-fs", NodeType.Server, 8,
            new() { 22, 445, 3389 }, new() { "ssh", "samba", "rdp" },
            new() { "eternalblue" }, new() { "10.0.1.200", "10.0.2.50", "172.16.0.1" },
            new() { "credentials.xlsx", "network_map.pdf" }, false, false, null, AccessLevel.None));

        nodes.Add(new("10.0.2.50", "hr-database", NodeType.Database, 9,
            new() { 22, 1433, 1521 }, new() { "ssh", "mssql", "oracle" },
            new() { "weak_sa_password" }, new() { "10.0.2.10" },
            new(), false, false, null, AccessLevel.None));

        nodes.Add(new("172.16.0.1", "darknet-gateway", NodeType.Darknet, 10,
            new() { 22, 80, 443, 6667, 9001 }, new() { "ssh", "http", "https", "irc", "tor" },
            new(), new() { "10.0.2.10", "172.16.0.50", "203.0.113.1" },
            new(), false, false, null, AccessLevel.None));

        nodes.Add(new("172.16.0.50", "marketplace", NodeType.Darknet, 12,
            new() { 80, 443 }, new() { "http", "https" },
            new() { "sqli" }, new() { "172.16.0.1", "172.16.0.100" },
            new() { "listings.db" }, false, false, null, AccessLevel.None));

        nodes.Add(new("172.16.0.100", "forum", NodeType.Darknet, 13,
            new() { 80, 443 }, new() { "http", "https" },
            new() { "xss", "csrf" }, new() { "172.16.0.50" },
            new() { "users.db" }, false, false, null, AccessLevel.None));

        nodes.Add(new("203.0.113.1", "backbone-router", NodeType.Router, 14,
            new() { 22, 443 }, new() { "ssh", "https" },
            new() { "backdoor" }, new() { "172.16.0.1", "198.51.100.1", "203.0.113.50" },
            new(), false, false, null, AccessLevel.None));

        nodes.Add(new("203.0.113.50", "gov-server", NodeType.Server, 17,
            new() { 22, 443, 8443 }, new() { "ssh", "https", "alt-https" },
            new() { "zero_day" }, new() { "203.0.113.1" },
            new() { "classified.bin", "communications.enc" }, false, false, null, AccessLevel.None));

        nodes.Add(new("198.51.100.1", "isp-router", NodeType.Router, 11,
            new() { 22, 443 }, new() { "ssh", "https" },
            new() { "default_community_string" }, new() { "203.0.113.1", "10.0.3.1", "10.0.4.1" },
            new(), false, false, null, AccessLevel.None));

        nodes.Add(new("10.0.3.1", "cloud-gateway", NodeType.Cloud, 9,
            new() { 22, 443, 8443 }, new() { "ssh", "https", "alt-https" },
            new() { "misconfigured_s3" }, new() { "198.51.100.1", "10.0.3.100", "10.0.3.200" },
            new(), false, false, null, AccessLevel.None));

        nodes.Add(new("10.0.3.100", "cloud-server", NodeType.Cloud, 10,
            new() { 22, 80, 443, 3306 }, new() { "ssh", "http", "https", "mysql" },
            new() { "weak_ssh_key" }, new() { "10.0.3.1" },
            new() { "config.php", ".env" }, false, false, null, AccessLevel.None));

        nodes.Add(new("10.0.3.200", "ecommerce", NodeType.Server, 10,
            new() { 22, 80, 443 }, new() { "ssh", "http", "https" },
            new() { "sqli", "xss", "lfi" }, new() { "10.0.3.1" },
            new() { "products.db", "orders.db" }, false, false, null, AccessLevel.None));

        nodes.Add(new("10.0.4.1", "mil-router", NodeType.Router, 15,
            new() { 22, 443, 8443 }, new() { "ssh", "https", "alt-https" },
            new() { "backdoor" }, new() { "198.51.100.1", "10.0.4.20", "10.0.5.1" },
            new(), false, false, null, AccessLevel.None));

        nodes.Add(new("10.0.4.20", "mil-server", NodeType.Server, 16,
            new() { 22, 443, 3389 }, new() { "ssh", "https", "rdp" },
            new() { "eternalblue", "doublepulsar" }, new() { "10.0.4.1" },
            new() { "drone_codes.bin", "patrol_schedules.pdf" }, false, false, null, AccessLevel.None));

        nodes.Add(new("10.0.5.1", "sat-uplink", NodeType.Satellite, 18,
            new() { 22, 443, 2100 }, new() { "ssh", "https", "satcom" },
            new() { "weak_encryption" }, new() { "10.0.4.1", "10.0.5.100" },
            new(), false, false, null, AccessLevel.None));

        nodes.Add(new("10.0.5.100", "corp-hq", NodeType.Server, 19,
            new() { 22, 443, 8080 }, new() { "ssh", "https", "proxy" },
            new() { "side_channel" }, new() { "10.0.5.1" },
            new() { "ceo_emails.enc", "financials.xlsx" }, false, false, null, AccessLevel.None));

        return nodes;
    }
}

public class NetworkService
{
    private readonly IPlayerService _playerService;
    private readonly IGameEventBus? _gameEventBus;
    private readonly Dictionary<string, NetworkNode> _networkNodes;
    private readonly Dictionary<string, HashSet<string>> _playerDiscoveries = new();
    private readonly Dictionary<string, HashSet<string>> _playerCompromises = new();
    private readonly SemaphoreSlim _mutex = new(1, 1);

    public NetworkService(IPlayerService playerService, IGameEventBus? gameEventBus = null)
    {
        _playerService = playerService;
        _gameEventBus = gameEventBus;
        _networkNodes = NetworkTopology.GenerateInitialNodes().ToDictionary(n => n.Ip);

        _playerDiscoveries["default"] = new() { "127.0.0.1", "10.0.0.1", "10.0.0.2" };
    }

    public async Task<List<NetworkNode>> GetDiscoveredNodes(string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            var discovered = GetOrCreatePlayerDiscoveries(playerId);
            return _networkNodes.Values.Where(n => discovered.Contains(n.Ip)).ToList();
        }
        finally
        {
            _mutex.Release();
        }
    }

    public async Task<List<NetworkNode>> GetAllNodes()
    {
        await _mutex.WaitAsync();
        try
        {
            return _networkNodes.Values.ToList();
        }
        finally
        {
            _mutex.Release();
        }
    }

    public async Task<NetworkNode?> GetNode(string ip)
    {
        await _mutex.WaitAsync();
        try
        {
            return _networkNodes.GetValueOrDefault(ip);
        }
        finally
        {
            _mutex.Release();
        }
    }

    public async Task<ServiceResult<NetworkNode>> DiscoverNode(string playerId, string ip)
    {
        await _mutex.WaitAsync();
        try
        {
            if (!_networkNodes.TryGetValue(ip, out var node))
                return ServiceResult<NetworkNode>.Failure($"Node not found: {ip}");

            var discovered = GetOrCreatePlayerDiscoveries(playerId);
            var wasNew = discovered.Add(ip);
            if (wasNew)
                await _playerService.DiscoverNodeAsync(playerId, ip);

            if (wasNew && _gameEventBus != null)
                await _gameEventBus.Emit(new GameEvent { PlayerId = playerId, Type = AchievementEvent.NODE_DISCOVERED, Value = 1 });

            return ServiceResult<NetworkNode>.Success(node);
        }
        finally
        {
            _mutex.Release();
        }
    }

    public async Task<List<NetworkNode>> ScanSubnet(string playerId, string subnet)
    {
        await _mutex.WaitAsync();
        try
        {
            var player = _playerService.GetPlayer(playerId);
            if (player == null) return new();

            var subnetPrefix = subnet.Contains('/') ? subnet[..subnet.LastIndexOf('/')] : subnet;
            var cidrStr = subnet.Contains('/') ? subnet[(subnet.LastIndexOf('/') + 1)..] : "24";
            _ = int.TryParse(cidrStr, out _);

            var allowedNetworks = new[]
            {
                "127.0.0.0/8", "10.0.0.0/8", "172.16.0.0/12",
                "192.168.0.0/16", "198.51.100.0/24", "203.0.113.0/24"
            };

            var isAllowed = allowedNetworks.Any(prefix =>
            {
                var allowedPrefix = prefix[..prefix.LastIndexOf('/')];
                var allowedBase = allowedPrefix[..allowedPrefix.LastIndexOf('.')];
                var subnetBase = subnetPrefix.Contains('.') ? subnetPrefix[..subnetPrefix.LastIndexOf('.')] : "";
                return subnetBase.StartsWith(allowedBase);
            });

            if (!isAllowed && !player.DiscoveredNodes.Contains(subnetPrefix))
                return new();

            var discoveredBefore = GetOrCreatePlayerDiscoveries(playerId).Count;

            var subnetBase = subnetPrefix.Contains('.')
                ? subnetPrefix[..subnetPrefix.LastIndexOf('.')]
                : subnetPrefix;

            var found = _networkNodes.Values
                .Where(node =>
                {
                    var nodePrefix = node.Ip.Contains('.') ? node.Ip[..node.Ip.LastIndexOf('.')] : node.Ip;
                    return nodePrefix == subnetBase &&
                           !GetOrCreatePlayerDiscoveries(playerId).Contains(node.Ip);
                })
                .ToList();

            foreach (var node in found)
            {
                GetOrCreatePlayerDiscoveries(playerId).Add(node.Ip);
                await _playerService.DiscoverNodeAsync(playerId, node.Ip);
            }

            var newlyDiscovered = GetOrCreatePlayerDiscoveries(playerId).Count - discoveredBefore;
            if (newlyDiscovered > 0 && _gameEventBus != null)
                await _gameEventBus.Emit(new GameEvent { PlayerId = playerId, Type = AchievementEvent.NODE_DISCOVERED, Value = newlyDiscovered });

            return found;
        }
        finally
        {
            _mutex.Release();
        }
    }

    public async Task<ServiceResult<NetworkNode>> CompromiseNode(string playerId, string ip, AccessLevel level)
    {
        await _mutex.WaitAsync();
        try
        {
            if (!_networkNodes.TryGetValue(ip, out var node))
                return ServiceResult<NetworkNode>.Failure($"Node not found: {ip}");

            var discovered = GetOrCreatePlayerDiscoveries(playerId);
            if (!discovered.Contains(ip))
                return ServiceResult<NetworkNode>.Failure($"Node {ip} not discovered yet. Scan the network first.");

            GetOrCreatePlayerCompromises(playerId).Add(ip);

            var updatedNode = node with { IsCompromised = true, AccessLevel = level };
            _networkNodes[ip] = updatedNode;

            if (_gameEventBus != null)
                await _gameEventBus.Emit(new GameEvent { PlayerId = playerId, Type = AchievementEvent.NETWORK_COMPROMISED, Value = 1 });

            return ServiceResult<NetworkNode>.Success(updatedNode);
        }
        finally
        {
            _mutex.Release();
        }
    }

    public async Task<List<NetworkNode>> GetAccessibleNodes(string playerId)
    {
        await _mutex.WaitAsync();
        try
        {
            var compromised = GetOrCreatePlayerCompromises(playerId);
            return _networkNodes.Values.Where(n => compromised.Contains(n.Ip)).ToList();
        }
        finally
        {
            _mutex.Release();
        }
    }

    public async Task<List<string>> FindPath(string playerId, string targetIp)
    {
        await _mutex.WaitAsync();
        try
        {
            var discovered = GetOrCreatePlayerDiscoveries(playerId);
            if (!discovered.Contains(targetIp)) return new();

            var visited = new HashSet<string>();
            var path = new List<string>();

            bool Dfs(string current, string target)
            {
                if (current == target)
                {
                    path.Add(current);
                    return true;
                }
                if (visited.Contains(current)) return false;
                visited.Add(current);

                if (!_networkNodes.TryGetValue(current, out var node)) return false;

                foreach (var neighbor in node.ConnectedNodes)
                {
                    if (discovered.Contains(neighbor) && Dfs(neighbor, target))
                    {
                        path.Insert(0, current);
                        return true;
                    }
                }
                return false;
            }

            Dfs("127.0.0.1", targetIp);
            return path;
        }
        finally
        {
            _mutex.Release();
        }
    }

    public async Task<NetworkStats> GetNetworkStats()
    {
        await _mutex.WaitAsync();
        try
        {
            return new NetworkStats(
                _networkNodes.Count,
                _playerCompromises.Values.SelectMany(x => x).Distinct().Count(),
                _playerDiscoveries.Values.Sum(x => x.Count),
                _playerService.GetOnlinePlayers().Count
            );
        }
        finally
        {
            _mutex.Release();
        }
    }

    private HashSet<string> GetOrCreatePlayerDiscoveries(string playerId)
    {
        if (!_playerDiscoveries.TryGetValue(playerId, out var set))
        {
            set = new HashSet<string>();
            _playerDiscoveries[playerId] = set;
        }
        return set;
    }

    private HashSet<string> GetOrCreatePlayerCompromises(string playerId)
    {
        if (!_playerCompromises.TryGetValue(playerId, out var set))
        {
            set = new HashSet<string>();
            _playerCompromises[playerId] = set;
        }
        return set;
    }
}
