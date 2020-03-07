import com.soywiz.korlibs.*

apply<KorlibsPlugin>()

korlibs {
    dependencyProject(":korim")
//    dependencyNodeModule("canvas", npmCanvasVersion)
}
