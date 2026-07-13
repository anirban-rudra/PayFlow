# Notification Service

Owns user notifications and read/unread state.

## Responsibilities

- Consume successful transaction events from Kafka.
- Create sender and receiver notifications.
- Expose user notification feed.
- Mark unread notifications as read.
- Allow admins to send operational notifications.

## Gateway APIs

```text
GET   /api/notifications/{userId}
PATCH /api/notifications/{userId}/read
POST  /api/notifications                    ROLE_ADMIN
```

## Data Ownership

Flyway migrations create:

- `notifications`
- `read_at` for read/unread state
- user/read index for unread lookups

Unread means `read_at IS NULL`.

## Product Behavior

- Dashboard unread count is based on unread notifications only.
- Opening the Alerts page marks the current user's unread notifications as read.
- Notification read state is persisted, so refreshes do not restore old unread counts.

## Security Notes

- Users can read and mark only their own notifications.
- Admins can read/send across users.
- Normal users cannot send arbitrary notifications.

## Local Commands

```bash
../mvnw test
../mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The service listens on `8084`.

## Related Docs

- [Root README](../Readme.md)
- [Design Document](../docs/DESIGN.md)
