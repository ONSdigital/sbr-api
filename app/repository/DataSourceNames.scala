package repository

/*
 * final is required for Scala to emit bytecode that will be considered "constant",
 * so that it can be used in @Named annotations.
 */
object DataSourceNames {
  final val SbrCtrl = "sbr-ctrl"
  final val Vat = "vat"
  final val Paye = "paye"
}
