# reFrame starts its ASGI application through Uvicorn directly. The FastAPI
# command-line development tooling is not needed on the appliance and pulls in
# a sizeable collection of unrelated CLI dependencies.
RDEPENDS:${PN}:remove = "python3-fastapi-cli"
