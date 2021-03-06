package arch.domain

trait Model {
  type Entity
  type Id
  trait Identifiable[E <: Entity] {
    def id(a: Entity): Id
  }
}