# Global Flow Tests

Cross-module contract tests for PayFlow's end-to-end payment behavior.

## Purpose

The global tests verify that the main payment contract still holds across service models:

- transaction service creates a successful transfer
- transaction outbox event is generated
- reward consumer can process the event payload
- notification consumer can process the event payload

These tests are not a replacement for full Docker end-to-end tests, but they catch important cross-module contract breaks quickly.

## Run

From the repository root:

```bash
./mvnw test
```

Only this module:

```bash
../mvnw test
```

from `global-flow-tests`.

## Related Docs

- [Root README](../Readme.md)
- [Design Document](../docs/DESIGN.md)

