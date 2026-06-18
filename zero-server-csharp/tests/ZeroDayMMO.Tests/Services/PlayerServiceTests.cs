using ZeroDayMMO.Server.Config;
using ZeroDayMMO.Server.Security;
using ZeroDayMMO.Server.Services;
using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Tests.Services;

class FakePasswordHasher : IPasswordHasher
{
    public string Hash(string password) => BCrypt.Net.BCrypt.HashPassword(password);
    public bool Verify(string password, string hash) => BCrypt.Net.BCrypt.Verify(password, hash);
}

public class PlayerServiceTests
{
    private static PlayerService CreateWithTestData()
    {
        var service = new PlayerService(new FakePasswordHasher());
        service.CreatePlayer("TestUser", BCrypt.Net.BCrypt.HashPassword("correct_password"), "TestUser");
        service.CreatePlayer("AnotherUser", BCrypt.Net.BCrypt.HashPassword("another_password"), "Another");
        return service;
    }

    [Fact]
    public void Authenticate_ValidCredentials_ReturnsPlayer()
    {
        var service = CreateWithTestData();

        var result = service.Authenticate("TestUser", "correct_password");

        Assert.NotNull(result);
        Assert.Equal("TestUser", result.Username);
    }

    [Fact]
    public void Authenticate_InvalidPassword_ReturnsNull()
    {
        var service = CreateWithTestData();

        var result = service.Authenticate("TestUser", "wrong_password");

        Assert.Null(result);
    }

    [Fact]
    public void Authenticate_UnknownUser_ReturnsNull()
    {
        var service = CreateWithTestData();

        var result = service.Authenticate("NonExistentUser", "password");

        Assert.Null(result);
    }

    [Fact]
    public void CreatePlayer_AddsPlayer_ToDictionary()
    {
        var service = new PlayerService(new FakePasswordHasher());

        var player = service.CreatePlayer("NewPlayer", "hash_new", "NewPlayer");

        var retrieved = service.GetPlayer(player.Id);
        Assert.NotNull(retrieved);
        Assert.Equal("NewPlayer", retrieved.Username);
    }

    [Fact]
    public void GetPlayer_ReturnsCorrectPlayer()
    {
        var service = CreateWithTestData();

        var result = service.GetPlayerByUsername("AnotherUser");

        Assert.NotNull(result);
        Assert.Equal("AnotherUser", result.Username);
    }

    [Fact]
    public void GetPlayerByUsername_CaseInsensitive()
    {
        var service = CreateWithTestData();

        var result = service.GetPlayerByUsername("testuser");

        Assert.NotNull(result);
        Assert.Equal("TestUser", result.Username);
    }
}
