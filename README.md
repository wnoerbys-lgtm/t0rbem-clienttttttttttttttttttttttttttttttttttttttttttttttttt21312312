# Donut Auction Tracker

A client-side Fabric mod for Minecraft Java Edition 1.21.11. Use it to run a simple payment-based auction on Donut SMP.

## Features

- Press **O** to open the auction menu.
- Choose an item from your inventory and set a minimum bid.
- Start the auction. Incoming payment receipts appear in the feed; the highest payment at or above the minimum is shown as the leader.
- End the auction with the same button. Your minimum is saved in `config/paytracker.properties`.
- The mod only reads server receipt messages. It does not send commands or change payments.

The feed lasts for the active auction and resets when a new auction starts. Payments below the minimum appear in the feed but do not become the leading bid. End the auction manually.

## Build

Install JDK 21 and Gradle, then run:

```powershell
gradle build
```

The built mod is written to `build/libs/paytracker-1.0.0.jar`. Install Fabric Loader for Minecraft 1.21.11 and Fabric API for 1.21.11, then put the jar in your Minecraft `mods` folder.

## Payment message format

The tracker recognizes server game messages matching:

```text
<player> paid you $ <amount>
```

The message format can vary by server update. If your payment receipt looks different, share the exact in-game text and the matcher can be adjusted.
