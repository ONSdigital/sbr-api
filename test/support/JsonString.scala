package support

object JsonString {
  def string(name: String, value: String): Option[String] =
    Some(s""""$name":"$value"""")

  def optionalString(name: String, optValue: Option[String]): Option[String] =
    optValue.flatMap(string(name, _))

  def int(name: String, value: Int): Option[String] =
    Some(s""""$name":$value""")

  def optionalInt(name: String, optValue: Option[Int]): Option[String] =
    optValue.flatMap(int(name, _))

  def withValues(values: Option[String]*): String =
    values.flatten.mkString(",")

  def withObject(name: String, values: Option[String]*): String =
    s""""$name":${withObject(values: _*)}"""

  def withObject(values: Option[String]*): String =
    values.flatten.mkString("{", ",", "}")
}
