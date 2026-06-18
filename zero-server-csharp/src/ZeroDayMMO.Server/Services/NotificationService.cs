using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Services;

public interface INotificationService
{
    Notification AddNotification(string playerId, Notification notification);
    bool MarkRead(string playerId, string notificationId);
    int GetUnreadCount(string playerId);
    List<Notification> GetNotifications(string playerId, bool includeRead = true, int limit = 50);
}

public class NotificationService : INotificationService
{
    private readonly PlayerService _playerService;
    private readonly int _perPlayerCap;

    public NotificationService(PlayerService playerService, int perPlayerCap = 50)
    {
        _playerService = playerService;
        _perPlayerCap = perPlayerCap;
    }

    public Notification AddNotification(string playerId, Notification notification)
    {
        var player = _playerService.GetPlayer(playerId);
        if (player == null) throw new KeyNotFoundException("Player not found");

        lock (player.Notifications)
        {
            player.Notifications.Add(notification);
            Trim(player);
        }

        return notification;
    }

    public bool MarkRead(string playerId, string notificationId)
    {
        var player = _playerService.GetPlayer(playerId);
        if (player == null) return false;

        lock (player.Notifications)
        {
            var n = player.Notifications.FirstOrDefault(x => x.Id == notificationId);
            if (n == null) return false;
            n.Read = true;
            return true;
        }
    }

    public int GetUnreadCount(string playerId)
    {
        var player = _playerService.GetPlayer(playerId);
        if (player == null) return 0;

        lock (player.Notifications)
        {
            return player.Notifications.Count(n => !n.Read);
        }
    }

    public List<Notification> GetNotifications(string playerId, bool includeRead = true, int limit = 50)
    {
        var player = _playerService.GetPlayer(playerId);
        if (player == null) return new List<Notification>();

        lock (player.Notifications)
        {
            var filtered = includeRead
                ? player.Notifications.ToList()
                : player.Notifications.Where(n => !n.Read).ToList();

            if (filtered.Count <= limit) return filtered;
            return filtered.Skip(filtered.Count - limit).Take(limit).ToList();
        }
    }

    private void Trim(Player player)
    {
        if (player.Notifications.Count <= _perPlayerCap) return;

        var overflow = player.Notifications.Count - _perPlayerCap;
        var read = player.Notifications.Where(n => n.Read).ToList();
        var toRemove = read.Take(Math.Min(overflow, read.Count)).ToHashSet();

        if (toRemove.Count > 0)
            player.Notifications.RemoveAll(n => toRemove.Contains(n));

        while (player.Notifications.Count > _perPlayerCap)
        {
            player.Notifications.RemoveAt(0);
        }
    }
}
