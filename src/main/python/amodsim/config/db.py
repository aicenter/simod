
class Db:
    def __init__(self, properties: dict=None):
        self.db_name = properties.get("db_name")
        self.user = properties.get("user")
        self.password = properties.get("password")


        pass

