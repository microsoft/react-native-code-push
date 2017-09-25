require 'optparse'
require 'shellwords'
class ShellExecutor
	@@dry_run = false
	class << self
		def shared_instance
			unless @@dry_run
				MyExecutor.setup()
				@@dry_run
			end
		end

		# setup
		def setup (dry_run = false)
			@@dry_run = dry_run
		end

		def dry?
			puts "If statement is #{@@dry_run.to_s}"
			@@dry_run
		end

		def run_command_line(line)
			puts "I will perform <#{line}>"
			if dry?
				puts "I am on dry run!"
			else
				# if run
				result = %x(#{line})
				puts "result is:" + result.to_s
				if $? != 0
					puts "I fall down on < #{result} >\n! because of < #{$?} >"
					exit($?)
				end
				result
			end
		end
	end
end

class KeyParameters
	TYPES = [:rsa, :ec]
	SIZES = [256, 384, 512]
	attr_accessor :type, :size, :secret
	def initialize(type, size, secret = 'secret')
		self.type = type
		self.size = size
		self.secret = secret
	end
	def tool
		%q(openssl);
	end
	def curve_by_size(size)
		case size
		when 256
			"#{secp256k1}"
		when 384
			"#{secp384k1}"
		when 512
			"#{secp512k1}"
		end
	end
	def suppress_prompt(command)
		%Q(echo '#{secret}' | #{command})
	end
	def generate_key(name)
		case type
		when :rsa
			%Q(#{tool} genrsa -des3 -out #{name}.pem #{size} -passin 'password')
		when :ec
			%Q(#{tool} ecparam -name #{curve_name} -genkey -noout -out #{name}.pem)
		end
	end
	def output_key(type, access, generated_key_name, name)
		%Q(#{tool} #{type} #{access == 'private' ? '' : '-pubout' } -in #{generated_key_name}.pem -out #{name}.pem < echo "#{secret}")
	end
	def output_public_key(generated_key_name, name)
		output_key(type, 'public', generated_key_name, name)
	end
	def output_private_key(generated_key_name, name)
		output_key(type, 'private', generated_key_name, name)
	end
end

def fix_options(the_options)
	options = the_options
	options[:result_directory] ||= '../Tests/Resources/Certs/'
	if options[:test]
		options[:algorithm_type] ||= KeyParameters::TYPES.first
		options[:key_length] ||= KeyParameters::SIZES.first
		options[:generated_key_name] ||= 'generated'
		options[:private_key_name] ||= 'private'
		options[:public_key_name] ||= 'public'
	end
	options
end

def MainWork(options = {})
	options = fix_options(options)

	if options[:inspection]
		puts "options are: #{options}"
	end

	ShellExecutor.setup options[:dry_run]

	key_parameters = KeyParameters.new(options[:algorithm_type], options[:key_length])
	[
		key_parameters.generate_key(options[:generated_key_name]),
		key_parameters.output_private_key(options[:generated_key_name], options[:private_key_name]),
		key_parameters.output_public_key(options[:generated_key_name], options[:public_key_name])
	].map do |command|
		key_parameters.suppress_prompt command
	end
	.each do |command|
		ShellExecutor.run_command_line command
	end
end

# ------------------- Beginning ----------------- #
def HelpMessage(options)

	# %x[rdoc $0]
	# not ok
	puts <<-__HELP__

	#{options.help}

	this script will help you generate keys.

	First, it takes arguments:
	[needed] <-f DIRECTORY>: directory where you will gather files
	[not needed] <-r DIRECTORY>: directory where files will be placed


	---------------
	Usage:
	---------------
	#{$0} -t ../Tests/Resources/Certs/

	__HELP__

end

options = {}


OptionParser.new do |opts|
	opts.banner = "Usage: #{$0} [options]"
	opts.on('-o', '--output_directory DIRECTORY', 'Output Directory') {|v| options[:output_directory] = v}
	opts.on('-t', '--test', 'Test option') {|v| options[:test] = v}
	opts.on('-l', '--length LENGTH', 'Key length') {|v| options[:key_length] = v}
	opts.on('-a', '--algorithm ALGORITHM', 'Algorithm type') {|v| options[:algorithm_type] = v}
	opts.on('-g', '--generated_key_name NAME', 'Generated key name') {|v| options[:generated_key_name] = v}
	opts.on('-r', '--private_key_name NAME', 'Private Key Name') {|v| options[:private_key_name] = v}
	opts.on('-u', '--public_key_name NAME', 'Public Key Name') {|v| options[:public_key_name] = v}
	# opts.on('-l', '--log_level LEVEL', 'Logger level of warning') {|v| options[:log_level] = v}
	# opts.on('-o', '--output_log OUTPUT', 'Logger output stream') {|v| options[:output_stream] = v}
	opts.on('-d', '--dry_run', 'Dry run to see all options') {|v| options[:dry_run] = v}
	opts.on('-i', '--inspection', 'Inspection of all items, like tests'){|v| options[:inspection] = v}

	# help
	opts.on('-h', '--help', 'Help option') { HelpMessage(opts); exit()}
end.parse!

MainWork(options)
