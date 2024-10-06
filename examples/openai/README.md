
```mermaid
sequenceDiagram
    UI->>Zillabase: transfer request
    Zillabase->>OpenAI: promt for fraud risk
    OpenAI->>Zillabase: capture fraud risk
    Zillabase->>UI: show requested user fraud risk
    UI->>Zillabase: accept transfer
    Zillabase->>OpenAI: add decision as less risk
    UI->>Zillabase: reject transfer
    Zillabase->>OpenAI: add decision as more risk
```
