# ZeroDayMMO Configuration

This directory contains all configuration files for the ZeroDayMMO server and related services.

## Directory Structure

- `server/` - Main server configuration
- `admin/` - Admin panel configuration
- `db/` - Database configuration
- `imagegen/` - Image generation configuration

## Configuration Files

### Server Configuration (`server/application.conf`)
Configures the main game server settings including:
- Network settings (ports, connection limits)
- Game mechanics (player limits, resource settings)
- Server identity

### Admin Configuration (`admin/admin.conf`)
Configures the admin panel including:
- Admin credentials
- Admin server settings
- Security settings
- Log retention

### Database Configuration (`db/database.conf`)
Configures the database connection including:
- Database URL
- Connection pool settings

### Image Generation Configuration (`imagegen/imagegen.conf`)
Configures the image generation system including:
- Output settings
- Generation parameters
- Perchance integration

## Environment Variables

The `.env` file contains environment variables that override the default configuration values. This file is loaded by the startup script.

## Starting the Server

Use the `start_server.sh` script to start the server with the proper configuration:

```bash
cd /root/ZeroDayMMO/config
./start_server.sh
```