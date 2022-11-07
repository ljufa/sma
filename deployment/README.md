- create `.env` file with twitter bearer token
```bash
TWITTER__AUTH_TOKEN=<paste your token here>
```

- run containers using docker hub repository
`make redeploy_from_docker_hub`
  
- build and run local containers
`make redeploy_local`

- open `http://localhost:8080` in the browser