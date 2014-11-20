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
    "provider" -> Provider name
    "parameters" -> JsonObject, used by each implementation differently. (depends on the action)

}
```

## Providers

### OVH

Specific json configuration :

```
{
    "main":"fr.wseduc.smsproxy.sms.Sms",
    "providers"{
        "OVH": {
            "applicationKey": "",
            "applicationSecret": "",
            "consumerKey": ""
        }
    }
}
```

Check the following link for more details on the API : [OVH API](https://eu.api.ovh.com/)
