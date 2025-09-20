# Modbus Data Reader

## Introduction

Modbus Data Reader is a Spring Boot backend service designed for industrial and measurement systems.
It connects to devices via TCP Modbus to read registers, performs calculations and measurements on the
retrieved data, and broadcasts real-time values to clients over WebSocket. Additionally, it logs all
measurement data into a MySQL database for historical tracking and analysis.

This tool is ideal for applications such as monitoring systems, dashboards, and data logging solutions
that require near real-time Modbus data acquisition and processing.

## Features

- Read measurement data from devices via TCP Modbus
- Broadcast live data to WebSocket clients
- Store measurements in a MySQL database
- Built using Spring Boot and JPA

## Getting Started

### Prerequisites

- Java JDK 17 or higher installed
- Maven installed (if you don’t have it, download it
  from [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi))
- MySQL 8.x
- Node.js and npm

## Build and Run

1. Open a terminal and navigate to the project root directory.

2. Build the project:

   ```bash
   mvn clean install
   mvn spring-boot:run

The application will start on http://localhost:8080/ by default.

## MySQL Database Configuration

Add the following properties to your `application.properties` or `application.yml` file:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/modbusdb
# Replace 'localhost' with your MySQL server IP or hostname
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
```

## Modbus TCP settings

The project uses Modbus4J to establish TCP connections to Modbus devices.
The connection configuration is handled in code using the following component structure:

Code-Based Configuration

Modbus devices are set up using a hierarchy of Java classes:

SubDevice – Logical unit within a device (e.g., energy, gas, heating, chp)

ModbusDevice – Represents a physical TCP Modbus device (IP, port, subDevices)

TestStation – Logical group of multiple Modbus devices

TestStationConfig – Defines the configuration of all test stations and their devices

ModbusClientService – Initializes and manages all Modbus TCP connections at runtime

🧩 Example Structure

// Define sub-device (e.g. energy meter)
SubDevice energy1 = new SubDevice(1000, 1, 4, SubDeviceType. ENERGY);


**Parameters explained:**

- `1000` — Start Address: The starting Modbus register address to read from
- `1` — Slave ID: The Modbus slave ID (unit identifier) of the device
- `4` — Registers Quantity: Number of Modbus registers to read starting from the start address
- `SubDeviceType.ENERGY` — The type/category of the sub-device (e.g., energy measurement)


energy1.addEnergyCalculationType(EnergyCalculationType.GENERATED_ENERGY);

// Define physical Modbus device

ModbusDevice device1 = new ModbusDevice("192.168.125.56", 502, List.of(energy1));

// Group into a test station

TestStation testStation1 = new TestStation(1, "Test Stand 1", List.of(device1));


🔄 Runtime Initialization

Connections are initialized automatically by the ModbusClientService using Modbus4J:

ModbusMaster modbusMaster = modbusFactory.createTcpMaster(params, true);
modbusMaster.setTimeout(5000);
modbusMaster.setRetries(3);
modbusMaster.init();

Each TestStation manages its own list of ModbusMaster connections, stored and reused efficiently.

✅ To add a new device, simply:

Create new SubDevices

Wrap them in a new ModbusDevice (with its IP and port)

Add them to a TestStation in TestStationConfig


# WebSocket endpoint

websocket.endpoint=/ws/data # Replace with your data


## WebSocket Data Format

Each WebSocket message is a JSON object structured as follows:


### 1. Single Measurement Update

A typical update for a single measurement from a device (identified by `testStationId`):

```json
{
  "GENERATED_ENERGY": 234.56,
  "testStationId": 2
}
```
### 2. Measurement Update with Difference
```json
{
  "GENERATED_ENERGY": "123.46",
  "testStationId": 2,
  "difference": "GENERATED_ENERGY"
}
```
### 3. Batch of Updates
```json
[
  {
    "GENERATED_ENERGY": "123.46",
    "testStationId": 2,
    "difference": "GENERATED_ENERGY"
  },
  {
    "CURRENT_POWER": "89.50",
    "testStationId": 2
  },
  {
    "HEAT_USAGE": "44.20",
    "testStationId": 3,
    "difference": "HEAT_USAGE"
  }
]
```
###  4. Initial Data Snapshot
```json
{
  "initialData": {
    "GENERATED_ENERGY": 12000.45,
    "HEATING_USAGE": 340.5,
    "GAS_FLOW": 12.75,
    "CHP_OUTPUT": 45.0
  },
  "startTime": 1694351223000
}
```
### 5. Error Message
```json
{
  "error": "WaitingRoomException trat beim Lesen der Register auf  "
}
```