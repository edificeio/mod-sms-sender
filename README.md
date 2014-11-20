# Vertx sms-proxy

This vert.x module can be used to send sms using various sms provders.

## Usage

This module registers itself on the vert.x bus and waits for a `Message<JsonObject>`.
Its address is "entcore.sms" by default, this can be changed by defining the field "sms-address" in the module configuration file.

Below is a short description of the json message syntax :

```
{
    "action" -> Action field, describes which action to perform
                List of all available actions :
                  - "send-sms" : sends a text message.
    "parameters" -> JsonObject, used by each implementation differently. (depends on the action)

}
```

## Providers

### OVH

Specific json configuration :

```
{
    "main":"org.vertx.smsproxy.ovh.OVHSms",
    "applicationKey": "{OVH APPLICATION KEY}",
    "applicationSecret": "{OVH APPLICATION SECRET}",
    "consumerKey": "{OVH CONSUMER KEY}"
}
```

See the following link for more details on the API : [OVH API](https://eu.api.ovh.com/)
