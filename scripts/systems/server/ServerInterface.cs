using System;
using System.Collections.Generic;
using Godot;

/// <summary>
/// ServerInterface is the Godot-side entry point for communicating with the C# server.
/// It wraps CommandHandler and exposes a method callable from GDScript.
/// </summary>
public class ServerInterface : Node
{
    private readonly CommandHandler _handler = new CommandHandler();

    /// <summary>
    /// Called from GDScript to execute a command on the server.
    /// Returns a Dictionary with 'output' and optional 'error'.
    /// </summary>
    public Dictionary ExecuteCommand(string command)
    {
        if (string.IsNullOrWhiteSpace(command))
        {
            return new Dictionary
            {
                ["output"] = "",
                ["error"] = "Empty command"
            };
        }

        // Dispatch to the command handler
        var result = _handler.ProcessCommand(command);

        // If the handler didn't return data, we need to fetch the response from its internal state.
        // For handlers that set global UI text, we'll construct a response based on that.
        // For simplicity, let's assume ProcessCommand always returns a full response.
        // Update: ProcessCommand now returns the response directly after each handler.

        if (result == null || result.Count == 0)
        {
            // Fallback: return what's in the global status/help text if applicable
            // This handles commands like 'status' or 'help' that set global vars.
            result = new Dictionary
            {
                ["output"] = GetGlobalText(),
                ["status"] = "success"
            };
        }

        return result;
    }

    // Helpers to retrieve global text set by handlers
    private string GetGlobalText()
    {
        // In a real implementation, this might read from a shared memory or Godot singleton.
        // For now, return an empty string.
        return "";
    }
}