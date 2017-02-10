package grails.gorm.services

import grails.gorm.multitenancy.TenantService
import grails.gorm.transactions.ReadOnly
import grails.gorm.transactions.TransactionService
import grails.gorm.transactions.Transactional
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.services.DefaultServiceRegistry
import org.grails.datastore.mapping.services.ServiceRegistry
import spock.lang.Specification

import java.lang.reflect.Type

/**
 * Created by graemerocher on 11/01/2017.
 */
class ServiceTransformSpec extends Specification {

    void "test count method"() {
        when:"The service transform is applied to an interface it can't implement"
        Class service = new GroovyClassLoader().parseClass('''
import grails.gorm.services.*
import grails.gorm.annotation.Entity
import static javax.persistence.criteria.JoinType.*

@Service(Foo)
interface MyService {
    Number count(String title)
    
    Integer countFoos(String title)
}
@Entity
class Foo {
    String title
    String name
}

''')

        then:
        service.isInterface()
        println service.classLoader.loadedClasses

        when:"the impl is obtained"
        Class impl = service.classLoader.loadClass("\$MyServiceImplementation")

        then:"The impl is valid"
        impl.getMethod("count", String).getAnnotation(ReadOnly) != null
        impl.genericInterfaces.find() { Type t -> t.typeName == "org.grails.datastore.mapping.services.Service<Foo>" }
    }

    void "test countBy method"() {
        when:"The service transform is applied to an interface it can't implement"
        Class service = new GroovyClassLoader().parseClass('''
import grails.gorm.services.*
import grails.gorm.annotation.Entity
import static javax.persistence.criteria.JoinType.*

@Service(Foo)
interface MyService {
    Number countByTitle(String t)
    
    Integer countName(String n)
}
@Entity
class Foo {
    String title
    String name
}

''')

        then:
        service.isInterface()
        println service.classLoader.loadedClasses

        when:"the impl is obtained"
        Class impl = service.classLoader.loadClass("\$MyServiceImplementation")

        then:"The impl is valid"
        impl.getMethod("countByTitle", String).getAnnotation(ReadOnly) != null
        impl.genericInterfaces.find() { Type t -> t.typeName == "org.grails.datastore.mapping.services.Service<Foo>" }
    }

    void "test simple list method"() {
        when:"The service transform is applied to an interface it can't implement"
        Class service = new GroovyClassLoader().parseClass('''
import grails.gorm.services.*
import grails.gorm.annotation.Entity
import static javax.persistence.criteria.JoinType.*

@Service(Foo)
interface MyService {
    List<Foo> listFoos()
    
}
@Entity
class Foo {
    String title
}

''')

        then:
        service.isInterface()
        println service.classLoader.loadedClasses

        when:"the impl is obtained"
        Class impl = service.classLoader.loadClass("\$MyServiceImplementation")

        then:"The impl is valid"
        impl.getMethod("listFoos").getAnnotation(ReadOnly) != null
        impl.genericInterfaces.find() { Type t -> t.typeName == "org.grails.datastore.mapping.services.Service<Foo>" }
    }

    void "test @Join on finder"() {
        when:"The service transform is applied to an interface it can't implement"
        Class service = new GroovyClassLoader().parseClass('''
import grails.gorm.services.*
import grails.gorm.annotation.Entity
import static javax.persistence.criteria.JoinType.*

@Service(Foo)
interface MyService {
    @Join('bars')
    Foo find(String title)
    
    @Join(value='bars', type=LEFT)
    Foo findFoo(String title)
    
}
@Entity
class Foo {
    String title
    static hasMany = [bars:Bar]
}
@Entity
class Bar {
    
}

''')

        then:
        service.isInterface()
        println service.classLoader.loadedClasses

        when:"the impl is obtained"
        Class impl = service.classLoader.loadClass("\$MyServiceImplementation")

        then:"The impl is valid"
        impl.genericInterfaces.find() { Type t -> t.typeName == "org.grails.datastore.mapping.services.Service<Foo>" }
    }

    void "test @Query invalid property"() {
        when:"The service transform is applied to an interface it can't implement"
        new GroovyClassLoader().parseClass('''
import grails.gorm.services.*
import grails.gorm.annotation.Entity

@Service(Foo)
interface MyService {

    @Query("from $Foo as f where f.title like $wrong") 
    Foo searchByTitle(String pattern)
}
@Entity
class Foo {
    String title
}
''')

        then:"A compilation error occurred"
        def e = thrown(MultipleCompilationErrorsException)
        e.message.contains '''[Static type checking] - The variable [wrong] is undeclared.
 @ line 8, column 48.
   $Foo as f where f.title like $wrong")'''
    }

    void "test @Query invalid domain"() {
        when:"The service transform is applied to an interface it can't implement"
        new GroovyClassLoader().parseClass('''
import grails.gorm.services.*
import grails.gorm.annotation.Entity

@Service(Foo)
interface MyService {

    @Query("from $String as f where f.title like $pattern") 
    Foo searchByTitle(String pattern)
}
@Entity
class Foo {
    String title
}
''')

        then:"A compilation error occurred"
        def e = thrown(MultipleCompilationErrorsException)
        e.message.contains '''Invalid query class [java.lang.String]. Referenced classes in queries must be domain classes
 @ line 8, column 19.
       @Query("from $String as f where f.title like $pattern") 
                     ^'''
    }

    void "test simple @Query annotation"() {
        when:"The service transform is applied to an interface it can't implement"
        Class service = new GroovyClassLoader().parseClass('''
import grails.gorm.services.*
import grails.gorm.annotation.Entity

@Service(Foo)
interface MyService {

    @Query("from $Foo as f where f.title like $pattern") 
    Foo searchByTitle(String pattern)
}
@Entity
class Foo {
    String title
}

''')

        then:
        service.isInterface()
        println service.classLoader.loadedClasses

        when:"the impl is obtained"
        Class impl = service.classLoader.loadClass("\$MyServiceImplementation")

        then:"The impl is valid"
        org.grails.datastore.mapping.services.Service.isAssignableFrom(impl)
    }


    void "test @Query annotation with declared variables"() {
        when:"The service transform is applied to an interface it can't implement"
        Class service = new GroovyClassLoader().parseClass('''
import grails.gorm.services.*
import grails.gorm.annotation.Entity

@Service(Foo)
interface MyService {

    @Query("from ${Foo f} where $f.title like $pattern") 
    Foo searchByTitle(String pattern)
}
@Entity
class Foo {
    String title
}

''')

        then:
        service.isInterface()
        println service.classLoader.loadedClasses

        when:"the impl is obtained"
        Class impl = service.classLoader.loadClass("\$MyServiceImplementation")

        then:"The impl is valid"
        org.grails.datastore.mapping.services.Service.isAssignableFrom(impl)
    }


    void "test @Query invalid variable property"() {
        when:"The service transform is applied to an interface it can't implement"
        new GroovyClassLoader().parseClass('''
import grails.gorm.services.*
import grails.gorm.annotation.Entity

@Service(Foo)
interface MyService {

    @Query("from ${Foo f} where $f.tit like $pattern") 
    Foo searchByTitle(String pattern)
}
@Entity
class Foo {
    String title
}
''')

        then:"A compilation error occurred"
        def e = thrown(MultipleCompilationErrorsException)
        e.message.contains '''No property [tit] existing for domain class [Foo]
 @ line 8, column 34.
       @Query("from ${Foo f} where $f.tit like $pattern") 
                                    ^'''
    }

    void 'test @Where annotation'() {
        when:"The service transform is applied to an interface it can't implement"
        Class service = new GroovyClassLoader().parseClass('''
import grails.gorm.services.*
import grails.gorm.annotation.Entity

@Service(Foo)
interface MyService {

    @Where({ title ==~ pattern  }) 
    Foo searchByTitle(String pattern)
}
@Entity
class Foo {
    String title
}

''')

        then:
        service.isInterface()
        println service.classLoader.loadedClasses

        when:"the impl is obtained"
        Class impl = service.classLoader.loadClass("\$MyServiceImplementation")

        then:"The impl is valid"
        org.grails.datastore.mapping.services.Service.isAssignableFrom(impl)
    }

    void "test implement abstract class"() {
        when:"The service transform is applied to an abstract class"
        Class service = new GroovyClassLoader().parseClass('''
import grails.gorm.services.*
import grails.gorm.annotation.Entity

@Service(Foo)
abstract class AbstractMyService implements MyService {

    Foo readFoo(Serializable id) {
        Foo.read(id)
    }
    
    @Override
    Foo delete(Serializable id) {
        def foo = Foo.get(id)
        foo?.delete()
        foo?.title = "DELETED"
        return foo
    }
}
 
interface MyService {
    Number deleteMoreFoos(String title)
    
    void deleteFoos(String title)
//    Foo get(Serializable id)
    
    Foo delete(Serializable id)
    
    List<Foo> listFoos()
    
    Foo[] listMoreFoos()
    
    Iterable<Foo> listEvenMoreFoos()
    
    List<Foo> findByTitle(String title)
    
    List<Foo> findByTitleLike(String title)
    
    Foo saveFoo(String title)
}
@Entity
class Foo {
    String title
}

''')
        then:
        !service.isInterface()
        println service.classLoader.loadedClasses

        when:"the impl is obtained"
        Class impl = service.classLoader.loadClass("\$AbstractMyServiceImplementation")

        then:"The impl is valid"
        impl.getMethod("deleteMoreFoos", String).getAnnotation(Transactional) != null
        impl.getMethod("delete", Serializable).getAnnotation(Transactional) != null
        impl.getMethod("readFoo", Serializable).getAnnotation(ReadOnly) != null
        impl.genericInterfaces.find() { Type t -> t.typeName == "org.grails.datastore.mapping.services.Service<Foo>" } != null
        org.grails.datastore.mapping.services.Service.isAssignableFrom(impl)

        when:
        impl.newInstance().listFoos()

        then:
        def e = thrown(IllegalStateException)
        e.message == 'No GORM implementations configured. Ensure GORM has been initialized correctly'
    }

    void "test implement list method"() {
        when:"The service transform is applied to an interface it can't implement"
        Class service = new GroovyClassLoader().parseClass('''
import grails.gorm.services.*
import grails.gorm.annotation.Entity

@Service(Foo)
interface MyService {
    Number deleteMoreFoos(String title)
    
    void deleteFoos(String title)
//    Foo get(Serializable id)
    
    Foo delete(Serializable id)
    
    List<Foo> listFoos()
    
    Foo[] listMoreFoos()
    
    Iterable<Foo> listEvenMoreFoos()
    
    List<Foo> findByTitle(String title)
    
    List<Foo> findByTitleLike(String title)
    
    Foo saveFoo(String title)
}
@Entity
class Foo {
    String title
}

''')

        then:
        service.isInterface()
        println service.classLoader.loadedClasses

        when:"the impl is obtained"
        Class impl = service.classLoader.loadClass("\$MyServiceImplementation")

        then:"The impl is valid"
        impl.getMethod("deleteMoreFoos", String).getAnnotation(Transactional) != null
        impl.genericInterfaces.find() { Type t -> t.typeName == "org.grails.datastore.mapping.services.Service<Foo>" } != null
        org.grails.datastore.mapping.services.Service.isAssignableFrom(impl)

        when:
        impl.newInstance().listFoos()

        then:
        def e = thrown(IllegalStateException)
        e.message == 'No GORM implementations configured. Ensure GORM has been initialized correctly'
    }

    void "test service transform applied to interface that can't be implemented"() {
        when:"The service transform is applied to an interface it can't implement"
        new GroovyClassLoader().parseClass('''
import grails.gorm.services.*

@Service
interface MyService {
    void foo()
}
''')

        then:"A compilation error occurred"
        def e = thrown(MultipleCompilationErrorsException)
        e.message.contains '''No implementations possible for method 'void foo()'. Please use an abstract class instead and provide an implementation.
 @ line 6, column 5.
       void foo()
       ^'''
    }

    void "test service transform applied with a dynamic finder for a non-existent property"() {
        when:"The service transform is applied to an interface it can't implement"
        new GroovyClassLoader().parseClass('''
import grails.gorm.services.*
import grails.gorm.annotation.Entity

@Service(expose=false)
interface MyService {
    
    List<Foo> findByTitLike(String title)
}
@Entity
class Foo {
    String title
}
''')

        then:"A compilation error occurred"
        def e = thrown(MultipleCompilationErrorsException)
        e.message.contains '''Cannot implement finder for non-existent property [tit] of class [Foo]
 @ line 8, column 5.
       List<Foo> findByTitLike(String title)'''
    }


    void "test service transform applied with a dynamic finder for a property of the wrong type"() {
        when:"The service transform is applied to an interface it can't implement"
        new GroovyClassLoader().parseClass('''
import grails.gorm.services.*
import grails.gorm.annotation.Entity

@Service(expose=false)
interface MyService {
    
    List<Foo> find(Integer title)
}
@Entity
class Foo {
    String title
}
''')

        then:"A compilation error occurred"
        def e = thrown(MultipleCompilationErrorsException)
        e.message.contains '''Cannot implement method for argument [title]. No property exists on domain class [Foo]
 @ line 8, column 5.
       List<Foo> find(Integer title)
       ^'''
    }

    void "test service transform"() {
        given:
        ServiceRegistry reg = new DefaultServiceRegistry(Mock(Datastore), false)

        expect:
        org.grails.datastore.mapping.services.Service.isAssignableFrom(TestService)
        reg.getService(TestService) != null
        reg.getService(TestService2) != null
        reg.getService(TestService).datastore != null
        reg.getService(TransactionService) != null
        reg.getService(TenantService) != null
    }
}

@Service
class TestService {
    void doStuff() {
    }
}

@Service
class TestService2 {
    void doStuff() {
    }
}
