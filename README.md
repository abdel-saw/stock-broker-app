```markdown
# Application de Diffusion des Cours Boursiers en Temps Réel avec Kafka et Spring Boot

[![Java](https://img.shields.io/badge/Java-21-blue)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-green)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.6.x-orange)](https://kafka.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue)](https://docs.docker.com/compose/)

## Description du Projet

Ce projet implémente une application distribuée de diffusion en temps réel des cours boursiers en utilisant **Apache Kafka** comme système de messagerie et **Spring Boot** pour le développement backend.

L'objectif principal est de permettre à plusieurs **courtiers** (clients simulés) de s'abonner à des titres boursiers spécifiques et de recevoir automatiquement les mises à jour de prix publiées par un producteur de données simulées.

L'architecture suit le modèle **publication/abonnement (pub/sub)** de Kafka, offrant une solution scalable et résiliente pour le traitement de flux de données financières.

> **Note** : Cette implémentation est une version simplifiée et fonctionnelle réalisée en moins de 12 heures, démontrant les concepts clés du projet. Elle peut être étendue (WebSocket, interface web, microservices séparés, etc.) comme décrit dans le rapport complet.

## Fonctionnalités Principales

- Génération automatique et périodique de cours boursiers simulés (AAPL, TSLA, MSFT, GOOGL, META, NVDA, etc.)
- Publication des mises à jour sur un topic Kafka (`stock-prices`)
- Simulation de plusieurs courtiers avec abonnements prédéfinis
- Filtrage en temps réel des messages reçus (seuls les courtiers abonnés à un symbole reçoivent les mises à jour)
- Affichage des publications et réceptions dans la console (pour démonstration)
- Configuration simple via Docker pour Kafka
- (Optionnel) Endpoint REST pour ajouter dynamiquement des abonnements

## Technologies Utilisées

- **Java 21**
- **Spring Boot 3.2.x** (avec Spring for Apache Kafka)
- **Apache Kafka 3.6.x** (via images Confluent)
- **Docker & Docker Compose** (pour lancer Kafka + Zookeeper)
- **Maven** (gestion des dépendances)
- **Lombok** (optionnel, pour réduire le code boilerplate)

## Prérequis

- Java 21 (JDK installé et configuré)
- Maven (ou utiliser le wrapper `./mvnw`)
- Docker Desktop (ou Docker Engine) installé et en cours d'exécution
- IDE recommandé : Eclipse (ou IntelliJ IDEA)

## Structure du Projet

```
stock-kafka-demo/
├── src/
│   ├── main/
│   │   ├── java/com/exemple/stockkafka/
│   │   │   ├── dto/                → PriceUpdate (record)
│   │   │   ├── producer/           → StockPriceSimulator (producteur)
│   │   │   ├── consumer/           → BrokerPriceListener (consommateur)
│   │   │   ├── controller/         → SubscriptionController (optionnel)
│   │   │   └── StockKafkaDemoApplication.java
│   │   └── resources/
│   │       └── application.properties  → Configuration Kafka
├── docker-compose.yml                  → Configuration Kafka + Zookeeper
├── pom.xml
└── README.md
```

## Installation et Configuration

### 1. Cloner ou créer le projet

Utilisez Spring Initializr (https://start.spring.io) avec :
- Java 21
- Maven
- Dépendances : Spring Web, Spring for Apache Kafka, Lombok (optionnel)

Ou importez directement le code fourni.

### 2. Configuration Kafka (`application.properties`)

```properties
spring.kafka.bootstrap-servers=localhost:9092

spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

spring.kafka.consumer.group-id=broker-clients
spring.kafka.consumer.auto-offset-reset=latest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=com.exemple.stockkafka.dto.*

server.port=8080
```

### 3. Lancer Kafka avec Docker

À la racine du projet, créez un fichier `docker-compose.yml` :

```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
```

Puis exécutez :

```bash
docker compose up -d
```

Vérifiez avec `docker ps` que les conteneurs `kafka` et `zookeeper` tournent.

### 4. Lancer l'application

Dans Eclipse :
- Clic droit sur `StockKafkaDemoApplication.java`
- **Run As → Spring Boot App**

Ou en ligne de commande :

```bash
./mvnw spring-boot:run
```

## Utilisation et Démonstration

Au démarrage, vous verrez dans la console :

```
PUBLISHED → PriceUpdate[symbol=AAPL, price=423.67, timestamp=...]
  [Courtier-Paris]  ←  AAPL = 423.67    (ts: ...)
  [Courtier-NewYork]  ←  AAPL = 423.67    (ts: ...)
```

Les abonnements par défaut sont :
- **Courtier-Paris** : AAPL, TSLA, LVMH
- **Courtier-London** : TSLA, BP, GS
- **Courtier-NewYork** : AAPL, MSFT, NVDA, GOOGL

### Ajouter un abonnement dynamiquement (optionnel)

Si vous avez activé le `SubscriptionController` :

```bash
curl -X POST "http://localhost:8080/api/subscribe?courtier=Courtier-Paris&symbol=NVDA"
```

## Extensions Possibles (pour approcher le rapport complet)

- Séparer producteur et consommateur en deux microservices
- Ajouter WebSocket (STOMP) pour une interface web temps réel
- Développer une page HTML/JS pour afficher les cours en direct
- Persistance des abonnements (base de données)
- Tests unitaires et d'intégration
- Déploiement complet avec Docker Compose (app + Kafka)

## Auteur

**SAWADOGO S. Abdel K Nourou**  
Cycle Ingénieur Génie Informatique  
Faculté des Sciences et Techniques de Settat  
Année Universitaire 2025-2026  

Encadré par : Mr Marzouk  

Date : Février 2026

## Références

- Rapport de projet complet : `Rapport_Projet_Courtier.pdf`
- Documentation Apache Kafka : https://kafka.apache.org/
- Spring for Apache Kafka : https://spring.io/projects/spring-kafka

---

Merci d'avoir utilisé ce projet ! Pour toute question ou amélioration, n'hésitez pas à contribuer.
```
```
