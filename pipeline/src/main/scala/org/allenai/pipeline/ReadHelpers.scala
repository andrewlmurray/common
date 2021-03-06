package org.allenai.pipeline

import spray.json.JsonFormat

import scala.io.Codec
import scala.reflect.ClassTag

import java.net.URI

trait ReadHelpers extends ColumnFormats {

  object Read {
    /** General deserialization method. */
    def fromArtifact[T, A <: Artifact](io: ArtifactIo[T, A], artifact: A): Producer[T] = {
      require(artifact.exists, s"$artifact does not exist")
      io match {
        case hasInfo: HasCodeInfo => new PersistedProducer(null, io, artifact) {
          override def signature: Signature = Signature(
            io.codeInfo.className,
            hasInfo.codeInfo.unchangedSince, "src" -> artifact.url
          )

          override def codeInfo: CodeInfo = hasInfo.codeInfo
        }
        case _ => new PersistedProducer(null, io, artifact)
      }
    }

    /** General deserialization method. */
    def fromArtifactProducer[T, A <: Artifact](
      io: ArtifactIo[T, A],
      src: Producer[A]
    ): Producer[T] =
      new Producer[T] {
        override def create: T = io.read(src.get)

        override def signature: Signature = Signature(
          io.codeInfo.className,
          io.codeInfo.unchangedSince,
          "src" -> src
        )

        override def codeInfo: CodeInfo = io.codeInfo

        override def outputLocation: Option[URI] = Some(src.get.url)
      }

    /** Read single object from flat file */
    object Singleton {
      def fromText[T: StringSerializable: ClassTag](artifact: FlatArtifact)(
        implicit
        codec: Codec
      ): Producer[T] =
        fromArtifact(SingletonIo.text[T], artifact)

      def fromJson[T: JsonFormat: ClassTag](artifact: FlatArtifact)(
        implicit
        codec: Codec
      ): Producer[T] =
        fromArtifact(SingletonIo.json[T], artifact)
    }

    /** Read collection of type T from flat file. */
    object Collection {
      def fromText[T: StringSerializable: ClassTag](artifact: FlatArtifact)(
        implicit
        codec: Codec
      ): Producer[Iterable[T]] =
        fromArtifact(LineCollectionIo.text[T], artifact)

      def fromJson[T: JsonFormat: ClassTag](artifact: FlatArtifact)(
        implicit
        codec: Codec
      ): Producer[Iterable[T]] =
        fromArtifact(LineCollectionIo.json[T], artifact)
    }

    /** Read iterator of type T from flat file. */
    object Iterator {
      def fromText[T: StringSerializable: ClassTag](artifact: FlatArtifact)(
        implicit
        codec: Codec
      ): Producer[Iterator[T]] =
        fromArtifact(LineIteratorIo.text[T], artifact)

      def fromJson[T: JsonFormat: ClassTag](artifact: FlatArtifact)(
        implicit
        codec: Codec
      ): Producer[Iterator[T]] =
        fromArtifact(LineIteratorIo.json[T], artifact)
    }

    /** Read a collection of arrays of a single type from a flat file. */
    object ArrayCollection {
      def fromText[T: StringSerializable: ClassTag](
        artifact: FlatArtifact,
        sep: Char = '\t'
      )(implicit codec: Codec): Producer[Iterable[Array[T]]] = {
        val io = {
          implicit val colFormat = columnArrayFormat[T](sep)
          implicit val arrayClassTag = implicitly[ClassTag[T]].wrap
          LineCollectionIo.text[Array[T]]
        }
        fromArtifact(io, artifact)
      }
    }

    /** Read an iterator of arrays of a single type from a flat file. */
    object ArrayIterator {
      def fromText[T: StringSerializable: ClassTag](
        artifact: FlatArtifact,
        sep: Char = '\t'
      )(implicit codec: Codec): Producer[Iterator[Array[T]]] = {
        val io = {
          implicit val colFormat = columnArrayFormat[T](sep)
          implicit val arrayClassTag = implicitly[ClassTag[T]].wrap
          LineIteratorIo.text[Array[T]]
        }
        fromArtifact(io, artifact)
      }
    }

  }

}
