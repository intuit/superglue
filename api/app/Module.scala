import Module.LineageServiceProvider
import com.google.inject.{AbstractModule, Provider}
import com.intuit.superglue.dao.SuperglueRepository
import com.intuit.superglue.lineage.LineageService
import com.typesafe.config.ConfigFactory
import javax.inject.Inject

class Module extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[SuperglueRepository]).toProvider(Module.RepositoryProvider.getClass)
    bind(classOf[LineageService]).toProvider(classOf[LineageServiceProvider])
  }
}

object Module {
  object RepositoryProvider extends Provider[SuperglueRepository] {
    override def get(): SuperglueRepository = {
      val config = ConfigFactory.load()
      SuperglueRepository(config).get
    }
  }

  class LineageServiceProvider @Inject()(repository: SuperglueRepository) extends Provider[LineageService] {
    override def get(): LineageService = {
      new LineageService(repository)
    }
  }
}
