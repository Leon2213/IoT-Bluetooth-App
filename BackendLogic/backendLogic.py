import paho.mqtt.client as mqtt
from re import search

macaddr1 = "14:3F:A6:91:5C.CA"
macaddr2 = "41:42:11:02:F3:2B"
macaddr3 = "28:F0:33:9F:4A:08"
data = ""

# The callback for when the client receives a CONNACK response from the server.

def on_connect(client, userdata, flags, rc):
    print("Connected with result code "+str(rc))
    # Subscribing in on_connect() means that if we lose the connection and
    # reconnect then subscriptions will be renewed.
    client.subscribe("bluetooth")


# The callback for when a PUBLISH message is received from the server.

def on_message(client, userdata, msg):

        rawdata = str(msg.payload)
        try:
            data = rawdata.replace("'","")
            data = data[1: : ]
            dataArray = data.split()
            id = dataArray[0]
            data = dataArray[1]
            print(id)
            print(data)
        except:
            return
        if search(macaddr1, data):
            print("yes, vi hittade: " + macaddr1)
            gustav = "Du är vid gustav adolfs torg! Platsen byggdes 1883 av Sve>
            print(gustav)
            client.publish(id, gustav)
        elif search(macaddr2, data):
            skeppsholmen = "Du är just nu vid skeppsholmen där platser som mode>
            print(skeppsholmen)
            client.publish(id, skeppsholmen)
        elif search(macaddr3, data):
            globen = "Du är just nu vid globen"
            print(globen)
            client.publish(id, globen)
        else:
            return

client = mqtt.Client()
client.on_connect = on_connect
client.on_message = on_message
client.connect("127.0.0.1", 1883, 60)

client.loop_forever()
