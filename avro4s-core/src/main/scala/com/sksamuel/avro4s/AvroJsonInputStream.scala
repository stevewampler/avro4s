package com.sksamuel.avro4s

import java.io.InputStream

import com.sksamuel.avro4s.internal.Decoder
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DecoderFactory

import scala.util.Try

final case class AvroJsonInputStream[T](in: InputStream,
                                        decoder: Decoder[T],
                                        writerSchema: Schema,
                                        readerSchema: Schema) extends AvroInputStream[T] {

  private val datumReader = new DefaultAwareDatumReader[GenericRecord](writerSchema, readerSchema, new DefaultAwareGenericData)
  private val jsonDecoder = DecoderFactory.get.jsonDecoder(writerSchema, in)

  private def next = Try {
    datumReader.read(null, jsonDecoder)
  }

  def iterator: Iterator[T] = Iterator.continually(next)
    .takeWhile(_.isSuccess)
    .map(_.get)
    .map(decoder.decode)

  def tryIterator: Iterator[Try[T]] = Iterator.continually(next)
    .takeWhile(_.isSuccess)
    .map(_.get)
    .map(record => Try(decoder.decode(record)))

  def singleEntity: Try[T] = next.map(decoder.decode)

  override def close(): Unit = in.close()
}