# CalenderApp
Log school, job, main course and workouts

## Run with Postgres (Docker)

This runs the frontend + backend with Postgres (persistent), Redis, and a 3-node Kafka cluster.

```sh
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Postgres: localhost:5432 (dev convenience)
- Redis: localhost:6379 (dev convenience)
- Kafka brokers (dev convenience): localhost:9092 / 9093 / 9094
- Data persistence: Docker volume `calenderapp_pgdata`

### Notes

- Change `APP_JWT_SECRET` in `docker-compose.yml` before real use (must be 32+ chars).

## Build + push images to Docker Hub

Login once:

```sh
docker login
```

Build and tag:

```sh
docker build -t thomastolo/calenderapp-backend:latest ./backend
docker build -t thomastolo/calenderapp-frontend:latest ./frontend
```

Push:

```sh
docker push thomastolo/calenderapp-backend:latest
docker push thomastolo/calenderapp-frontend:latest
```


