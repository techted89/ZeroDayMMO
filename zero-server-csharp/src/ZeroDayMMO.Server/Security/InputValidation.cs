using System.Text.RegularExpressions;

namespace ZeroDayMMO.Server.Security;

public static partial class InputValidator
{
    private static readonly Regex UsernameRegex = MyRegex();
    private const int PasswordMin = 8;
    private const int PasswordMax = 128;
    private const int TextMax = 200;
    private static readonly Regex ControlChars = ControlCharsRegex();

    private static readonly HashSet<string> ReservedUsernames = new(StringComparer.OrdinalIgnoreCase)
    {
        "admin", "root", "system", "moderator", "support", "zeroday"
    };

    public sealed record ValidationResult
    {
        public static readonly ValidationResult Ok = new() { IsValid = true };
        public bool IsValid { get; init; }
        public string? Reason { get; init; }

        public static ValidationResult Invalid(string reason) => new() { IsValid = false, Reason = reason };
    }

    public static ValidationResult ValidateUsername(string raw)
    {
        if (string.IsNullOrEmpty(raw))
            return ValidationResult.Invalid("Username required");
        if (raw.Length > 24)
            return ValidationResult.Invalid("Username too long (max 24 chars)");
        if (!UsernameRegex.IsMatch(raw))
            return ValidationResult.Invalid(
                "Username must be 3-24 chars, alphanumeric/_/-, starting with a letter or digit");
        return ValidationResult.Ok;
    }

    public static ValidationResult ValidatePassword(string raw)
    {
        if (raw.Length < PasswordMin)
            return ValidationResult.Invalid($"Password must be at least {PasswordMin} characters");
        if (raw.Length > PasswordMax)
            return ValidationResult.Invalid($"Password too long (max {PasswordMax})");
        return ValidationResult.Ok;
    }

    public static ValidationResult ValidateText(string raw, int max = TextMax, int min = 1)
    {
        if (raw.Length < min)
            return ValidationResult.Invalid($"Text too short (min {min})");
        if (raw.Length > max)
            return ValidationResult.Invalid($"Text too long (max {max})");
        if (ControlChars.IsMatch(raw))
            return ValidationResult.Invalid("Text contains control characters");
        return ValidationResult.Ok;
    }

    public static bool IsReservedUsername(string raw) =>
        ReservedUsernames.Contains(raw);

    [GeneratedRegex("^[A-Za-z0-9][A-Za-z0-9_\\-]{2,23}$")]
    private static partial Regex MyRegex();

    [GeneratedRegex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]")]
    private static partial Regex ControlCharsRegex();
}
