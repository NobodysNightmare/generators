# -*- coding: utf-8 -*-

# IO-4 Bricklet communication config

com = {
    'author': 'Olaf Lüke <olaf@tinkerforge.com>',
    'api_version': [2, 0, 0],
    'category': 'Bricklet',
    'device_identifier': 29,
    'name': ('IO4', 'io4', 'IO-4'),
    'manufacturer': 'Tinkerforge',
    'description': 'Device for controlling up to 4 general purpose input/output pins',
    'packets': []
}

com['packets'].append({
'type': 'function',
'name': ('SetValue', 'set_value'),
'elements': [('value_mask', 'uint8', 1, 'in')],
'since_firmware': [1, 0, 0],
'doc': ['bf', {
'en':
"""
Sets the output value (high or low) with a bitmask. The bitmask
is 4 bit long, *true* refers to high and *false* refers to low.

For example: The value 0b0011 will turn the pins 0-1 high and the
pins 2-3 low.

.. note::
 This function does nothing for pins that are configured as input.
 Pull-up resistors can be switched on with :func:`SetConfiguration`.
""",
'de':
"""
Setzt den Ausgangszustand (logisch 1 oder logisch 0) mittels einer Bitmaske.
Die Bitmaske hat eine Länge von 4 Bit, *true* bedeutet logisch 1 und *false*
logisch 0.

Beispiel: Der Wert 0b0011 setzt die Pins 0-1 auf logisch 1 und die Pins 2-3
auf logisch 0.

.. note::
 Diese Funktion bewirkt keine Änderung an Pins die als Eingang konfiguriert sind.
 Pull-Up Widerstände können mit :func:`SetConfiguration` zugeschaltet werden.
"""
}]
})

com['packets'].append({
'type': 'function',
'name': ('GetValue', 'get_value'),
'elements': [('value_mask', 'uint8', 1, 'out')],
'since_firmware': [1, 0, 0],
'doc': ['bf', {
'en':
"""
Returns a bitmask of the values that are currently measured.
This function works if the pin is configured to input
as well as if it is configured to output.
""",
'de':
"""
Gibt eine Bitmaske der aktuell gemessenen Zustände zurück.
Diese Funktion gibt die Zustände aller Pins zurück, unabhängig ob diese als
Ein- oder Ausgang konfiguriert sind.
"""
}]
})

com['packets'].append({
'type': 'function',
'name': ('SetConfiguration', 'set_configuration'),
'elements': [('selection_mask', 'uint8', 1, 'in'),
             ('direction', 'char', 1, 'in', ('Direction', 'direction', [('In', 'in', 'i'),
                                                                        ('Out', 'out', 'o')])),
             ('value', 'bool', 1, 'in')],
'since_firmware': [1, 0, 0],
'doc': ['bf', {
'en':
"""
Configures the value and direction of the specified pins. Possible directions
are "i" and "o" for input and output.

If the direction is configured as output, the value is either high or low
(set as *true* or *false*).

If the direction is configured as input, the value is either pull-up or
default (set as *true* or *false*).

For example:

* (15, 'i', true) will set all pins of as input pull-up.
* (8, 'i', false) will set pin 3 of as input default (floating if nothing is connected).
* (3, 'o', false) will set pins 0 and 1 as output low.
* (4, 'o', true) will set pin 2 of as output high.
""",
'de':
"""
Konfiguriert den Zustand und die Richtung eines angegebenen Pins. Mögliche
Richtungen sind "i" und "o" für Ein- und Ausgang.

Wenn die Richtung als Ausgang konfiguriert ist, ist der Zustand entweder
logisch 1 oder logisch 0 (gesetzt als *true* oder *false*).

Wenn die Richtung als Eingang konfiguriert ist, ist der Zustand entweder
Pull-Up oder Standard (gesetzt als *true* oder *false*).

Beispiele:

* (15, 'i', true) setzt alle Pins als Eingang mit Pull-Up.
* (8, 'i', false) setzt Pin 3 als Standard Eingang (potentialfrei wenn nicht verbunden).
* (3, 'o', false) setzt die Pins 0 und 1 als Ausgang im Zustand logisch 0.
* (4, 'o', true) setzt Pin 2 als Ausgang im Zustand logisch 1.
"""
}]
})

com['packets'].append({
'type': 'function',
'name': ('GetConfiguration', 'get_configuration'),
'elements': [('direction_mask', 'uint8', 1, 'out'),
             ('value_mask', 'uint8', 1, 'out')],
'since_firmware': [1, 0, 0],
'doc': ['bf', {
'en':
"""
Returns a value bitmask and a direction bitmask.

For example: A return value of 0b0011 and 0b0101 for
direction and value means that:

* pin 0 is configured as input pull-up,
* pin 1 is configured as input default,
* pin 2 is configured as output high
* and pin 3 is are configured as output low.
""",
'de':
"""
Gibt eine Bitmaske für die Richtung und eine Bitmaske für den Zustand der Pins
zurück.

Beispiel: Ein Rückgabewert von 0b0011 und 0b0101 für Richtung und Zustand
bedeutet:

* Pin 0 ist als Eingang mit Pull-Up konfiguriert,
* Pin 1 ist als Standard Eingang konfiguriert,
* Pin 2 ist als Ausgang im Zustand logisch 1 konfiguriert
* und Pin 3 ist als Ausgang im Zustand logisch 0 konfiguriert.
"""
}]
})

com['packets'].append({
'type': 'function',
'name': ('SetDebouncePeriod', 'set_debounce_period'),
'elements': [('debounce', 'uint32', 1, 'in')],
'since_firmware': [1, 0, 0],
'doc': ['ccf', {
'en':
"""
Sets the debounce period of the :func:`Interrupt` callback in ms.

For example: If you set this value to 100, you will get the interrupt
maximal every 100ms. This is necessary if something that bounces is
connected to the IO-4 Bricklet, such as a button.

The default value is 100.
""",
'de':
"""
Setzt die Entprellperiode der :func:`Interrupt` Callback in ms.

Beispiel: Wenn dieser Wert auf 100 gesetzt wird, erhält man den Interrupt
maximal alle 100ms. Dies ist notwendig falls etwas prellendes an
das IO-4 Bricklet angeschlossen ist, wie z.B. eine Taste.

Der Standardwert ist 100.
"""
}]
})

com['packets'].append({
'type': 'function',
'name': ('GetDebouncePeriod', 'get_debounce_period'),
'elements': [('debounce', 'uint32', 1, 'out')],
'since_firmware': [1, 0, 0],
'doc': ['ccf', {
'en':
"""
Returns the debounce period as set by :func:`SetDebouncePeriod`.
""",
'de':
"""
Gibt die Entprellperiode zurück, wie von :func:`SetDebouncePeriod`
gesetzt.
"""
}]
})

com['packets'].append({
'type': 'function',
'name': ('SetInterrupt', 'set_interrupt'),
'elements': [('interrupt_mask', 'uint8', 1, 'in')],
'since_firmware': [1, 0, 0],
'doc': ['ccf', {
'en':
"""
Sets the pins on which an interrupt is activated with a bitmask.
Interrupts are triggered on changes of the voltage level of the pin,
i.e. changes from high to low and low to high.

For example: An interrupt bitmask of 9 will enable the interrupt for
pins 0 and 3.

The interrupt is delivered with the callback :func:`Interrupt`.
""",
'de':
"""
Setzt durch eine Bitmaske die Pins für welche der Interrupt aktiv ist.
Interrupts werden ausgelöst bei Änderung des Spannungspegels eines Pins,
z.B. ein Wechsel von logisch 1 zu logisch 0 und logisch 0 zu logisch 1.

Beispiel: Eine Interrupt Bitmaske von 9 aktiviert den Interrupt für die
Pins 0 und 3.

Der Interrupt wird mit der Callback :func:`Interrupt` zugestellt.
"""
}]
})

com['packets'].append({
'type': 'function',
'name': ('GetInterrupt', 'get_interrupt'),
'elements': [('interrupt_mask', 'uint8', 1, 'out')],
'since_firmware': [1, 0, 0],
'doc': ['ccf', {
'en':
"""
Returns the interrupt bitmask as set by :func:`SetInterrupt`.
""",
'de':
"""
Gibt die Interrupt Bitmaske zurück, wie von :func:`SetInterrupt` gesetzt.
"""
}]
})

com['packets'].append({
'type': 'callback',
'name': ('Interrupt', 'interrupt'),
'elements': [('interrupt_mask', 'uint8', 1, 'out'),
             ('value_mask', 'uint8', 1, 'out')],
'since_firmware': [1, 0, 0],
'doc': ['c', {
'en':
"""
This callback is triggered whenever a change of the voltage level is detected
on pins where the interrupt was activated with :func:`SetInterrupt`.

The values are a bitmask that specifies which interrupts occurred
and the current value bitmask.

For example:

* (1, 1) means that an interrupt on pin 0 occurred and
  currently pin 0 is high and pins 1-3 are low.
* (9, 14) means that interrupts on pins 0 and 3
  occurred and currently pin 0 is low and pins 1-3 are high.
""",
'de':
"""
Dieser Callback wird ausgelöst sobald eine Änderung des Spannungspegels
detektiert wird, an Pins für welche der Interrupt mit :func:`SetInterrupt`
aktiviert wurde.

Die Rückgabewerte sind eine Bitmaske der aufgetretenen Interrupts und der
aktuellen Zustände.

Beispiele:

* (1, 1) bedeutet, dass ein Interrupt am Pin 0 ist aufgetreten ist und aktuell
  Pin 0 logisch 1 ist und die Pins 1-3 logisch 0 sind.
* (9, 14) bedeutet, dass Interrupts an den Pins 0 und 3 aufgetreten sind und
  aktuell Pin 0 logisch 0 ist und die Pins 1-3 logisch 1 sind.
"""
}]
})

com['packets'].append({
'type': 'function',
'name': ('SetMonoflop', 'set_monoflop'),
'elements': [('selection_mask', 'uint8', 1, 'in'),
             ('value_mask', 'uint8', 1, 'in'),
             ('time', 'uint32', 1, 'in')],
'since_firmware': [1, 1, 1],
'doc': ['af', {
'en':
"""
Configures a monoflop of the pins specified by the first parameter as 4 bit
long bitmask. The specified pins must be configured for output. Non-output
pins will be ignored.

The second parameter is a bitmask with the desired value of the specified
output pins (*true* means high and *false* means low).

The third parameter indicates the time (in ms) that the pins should hold
the value.

If this function is called with the parameters ((1 << 0) | (1 << 3), (1 << 0), 1500):
Pin 0 will get high and pin 3 will get low. In 1.5s pin 0 will get low and pin
3 will get high again.

A monoflop can be used as a fail-safe mechanism. For example: Lets assume you
have a RS485 bus and an IO-4 Bricklet connected to one of the slave
stacks. You can now call this function every second, with a time parameter
of two seconds and pin 0 set to high. Pin 0 will be high all the time. If now
the RS485 connection is lost, then pin 0 will get low in at most two seconds.
""",
'de':
"""
Konfiguriert einen Monoflop für die Pins, wie mittels der 4 Bit langen Bitmaske
des ersten Parameters festgelegt. Die festgelegten Pins müssen als Ausgänge
konfiguriert sein. Als Eingänge konfigurierte Pins werden ignoriert.

Der zweite Parameter ist eine Bitmaske mit den gewünschten Zustanden der
festgelegten Ausgangspins (*true* bedeutet logisch 1 und *false* logisch 0).

Der dritte Parameter stellt die Zeit (in ms) dar, welche die Pins den Zustand
halten sollen.

Wenn diese Funktion mit den Parametern ((1 << 0) | (1 << 3), (1 << 0), 1500)
aufgerufen wird: Pin 0 wird auf logisch 1 und Pin 3 auf logisch 0 gesetzt.
Nach 1,5s wird Pin 0 wieder logisch 0 und Pin 3 logisch 1 gesetzt.

Ein Monoflop kann zur Ausfallsicherung verwendet werden. Beispiel:
Angenommen ein RS485 Bus und ein IO-4 Bricklet ist an ein Slave Stapel verbunden.
Jetzt kann diese Funktion sekündlich, mit einem Zeitparameter von 2 Sekunden,
aufgerufen werden. Der Pin wird die gesamte Zeit im Zustand logisch 1 sein.
Wenn jetzt die RS485 Verbindung getrennt wird, wird der Pin nach spätestens zwei
Sekunden in den Zustand logisch 0 wechseln.
"""
}]
})

com['packets'].append({
'type': 'function',
'name': ('GetMonoflop', 'get_monoflop'),
'elements': [('pin', 'uint8', 1, 'in'),
             ('value', 'uint8', 1, 'out'),
             ('time', 'uint32', 1, 'out'),
             ('time_remaining', 'uint32', 1, 'out')],
'since_firmware': [1, 1, 1],
'doc': ['af', {
'en':
"""
Returns (for the given pin) the current value and the time as set by
:func:`SetMonoflop` as well as the remaining time until the value flips.

If the timer is not running currently, the remaining time will be returned
as 0.
""",
'de':
"""
Gibt (für den angegebenen Pin) den aktuellen Zustand und die Zeit, wie von 
:func:`SetMonoflop` gesetzt, sowie die noch verbleibende Zeit bis zum
Zustandswechsel, zurück.

Wenn der Timer aktuell nicht läuft, ist die noch verbleibende Zeit 0.
"""
}]
})

com['packets'].append({
'type': 'callback',
'name': ('MonoflopDone', 'monoflop_done'),
'elements': [('selection_mask', 'uint8', 1, 'out'),
             ('value_mask', 'uint8', 1, 'out')],
'since_firmware': [1, 1, 1],
'doc': ['c', {
'en':
"""
This callback is triggered whenever a monoflop timer reaches 0. The
:word:`parameters` contain the involved pins and the current value of the pins
(the value after the monoflop).
""",
'de':
"""
Dieser Callback wird ausgelöst wenn ein Monoflop Timer abläuft (0 erreicht).
:word:`parameters` enthalten die beteiligten Pins als Bitmaske und den aktuellen
Zustand als Bitmaske (der Zustand nach dem Monoflop).
"""
}]
})

com['packets'].append({
'type': 'function',
'name': ('SetSelectedValues', 'set_selected_values'),
'elements': [('selection_mask', 'uint8', 1, 'in'),
             ('value_mask', 'uint8', 1, 'in')],
'since_firmware': [2, 0, 0],
'doc': ['af', {
'en':
"""
Sets the output value (high or low) with a bitmask, according to
the selection mask. The bitmask is 4 bit long, *true* refers to high 
and *false* refers to low.

For example: The values 0b0110, 0b0011 will turn pin 2 high and
pin 1 low, pin 0 and 3 will remain untouched.

.. note::
 This function does nothing for pins that are configured as input.
 Pull-up resistors can be switched on with :func:`SetConfiguration`.
""",
'de':
"""
Setzt den Ausgangszustand (logisch 1 oder logisch 0) mittels einer Bitmaske,
entsprechend der Selktionsmaske. Die Bitmaske hat eine Länge von 4 Bit, 
*true* bedeutet logisch 1 und *false*
logisch 0.

Beispiel: Die Werte 0b0110, 0b0011 setzen den Pin 2 auf logisch 1 und den Pin 1
auf logisch 0. Die Pins 0 und 3 bleiben unangetastet.

.. note::
 Diese Funktion bewirkt keine Änderung an Pins die als Eingang konfiguriert sind.
 Pull-Up Widerstände können mit :func:`SetConfiguration` zugeschaltet werden.
"""
}]
})

